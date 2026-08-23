//! Generic PMX secondary-motion fallback for models without MMD rigid bodies.
//!
//! Standard PMX rigid bodies and joints always take priority. This module only
//! builds conservative spring chains from the existing skeleton when a model
//! has no MMD physics data (a common result of FBX/VRChat -> PMX conversion).

use std::collections::{HashMap, HashSet};
use std::fs;
use std::path::Path;

use serde_json::Value;

use crate::model::{SpringBoneData, SpringBoneJoint, SpringBoneSpring};
use crate::skeleton::BoneManager;

#[derive(Clone, Copy, Debug)]
struct Profile {
    stiffness: f32,
    gravity_power: f32,
    drag_force: f32,
    hit_radius: f32,
}

impl Profile {
    const HAIR: Self = Self::new(0.80, 0.24, 0.45, 0.025);
    const TAIL: Self = Self::new(0.68, 0.14, 0.38, 0.035);
    const EAR: Self = Self::new(1.10, 0.08, 0.54, 0.025);
    const CLOTH: Self = Self::new(0.62, 0.34, 0.42, 0.025);
    const SOFT_BODY: Self = Self::new(0.55, 0.12, 0.50, 0.035);
    const ACCESSORY: Self = Self::new(0.82, 0.16, 0.48, 0.02);

    const fn new(stiffness: f32, gravity_power: f32, drag_force: f32, hit_radius: f32) -> Self {
        Self { stiffness, gravity_power, drag_force, hit_radius }
    }

    fn apply_json(mut self, value: Option<&Value>) -> Self {
        let Some(value) = value else { return self; };
        self.stiffness = number(value, "stiffness", self.stiffness).clamp(0.0, 8.0);
        self.gravity_power = number(value, "gravityPower", self.gravity_power).clamp(0.0, 8.0);
        self.drag_force = number(value, "dragForce", self.drag_force).clamp(0.0, 0.99);
        self.hit_radius = number(value, "hitRadius", self.hit_radius).clamp(0.0, 2.0);
        self
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
enum ProfileKind { Hair, Tail, Ear, Cloth, SoftBody, Accessory }

impl ProfileKind {
    fn key(self) -> &'static str {
        match self {
            Self::Hair => "hair", Self::Tail => "tail", Self::Ear => "ear",
            Self::Cloth => "cloth", Self::SoftBody => "softBody", Self::Accessory => "accessory",
        }
    }

    fn defaults(self) -> Profile {
        match self {
            Self::Hair => Profile::HAIR, Self::Tail => Profile::TAIL, Self::Ear => Profile::EAR,
            Self::Cloth => Profile::CLOTH, Self::SoftBody => Profile::SOFT_BODY,
            Self::Accessory => Profile::ACCESSORY,
        }
    }
}

#[derive(Debug)]
pub(crate) struct SecondaryMotionBuild {
    pub(crate) data: SpringBoneData,
    pub(crate) chain_count: usize,
    pub(crate) joint_count: usize,
}

pub(crate) fn build_secondary_motion(
    bones: &BoneManager,
    model_path: &Path,
) -> Option<SecondaryMotionBuild> {
    let config_path = model_path.with_file_name("secondary_motion.json");
    let config = read_config(&config_path);
    if config.as_ref().and_then(|v| v.get("enabled")).and_then(Value::as_bool) == Some(false) {
        return None;
    }

    let auto_detect = config.as_ref().and_then(|v| v.get("autoDetect"))
        .and_then(Value::as_bool).unwrap_or(true);
    let include_markers = string_array(config.as_ref().and_then(|v| v.get("includeMarkers")));
    let exclude_markers = string_array(config.as_ref().and_then(|v| v.get("excludeMarkers")));
    let profiles = profile_map(config.as_ref());

    let mut classified = vec![None; bones.bone_count()];
    if auto_detect {
        for (index, bone) in bones.links().enumerate() {
            let name = normalize(&bone.name);
            if !matches_any(&name, &exclude_markers) {
                classified[index] = classify(&name).or_else(|| {
                    matches_any(&name, &include_markers).then_some(ProfileKind::Accessory)
                });
            }
        }
    }

    let mut chains: Vec<(Vec<usize>, ProfileKind)> = Vec::new();
    collect_auto_chains(bones, &classified, &mut chains);
    collect_explicit_chains(bones, config.as_ref(), &mut chains);

    let mut seen = HashSet::new();
    chains.retain(|(chain, _)| chain.len() >= 2 && seen.insert(chain.clone()));

    let springs = chains.iter().filter_map(|(chain, kind)| {
        let profile = profiles.get(kind).copied().unwrap_or_else(|| kind.defaults());
        let joints = chain.iter().copied().map(|node| SpringBoneJoint {
            node,
            hit_radius: profile.hit_radius,
            stiffness: profile.stiffness,
            gravity_power: profile.gravity_power,
            gravity_dir: [0.0, -1.0, 0.0],
            drag_force: profile.drag_force,
        }).collect::<Vec<_>>();
        (!joints.is_empty()).then_some(SpringBoneSpring {
            joints,
            collider_groups: Vec::new(),
            center: None,
        })
    }).collect::<Vec<_>>();

    let joint_count = springs.iter().map(|spring| spring.joints.len()).sum();
    (!springs.is_empty()).then_some(SecondaryMotionBuild {
        chain_count: springs.len(),
        joint_count,
        data: SpringBoneData { springs, colliders: Vec::new(), collider_groups: Vec::new() },
    })
}

fn read_config(path: &Path) -> Option<Value> {
    if !path.is_file() { return None; }
    match fs::read_to_string(path).ok().and_then(|text| serde_json::from_str(&text).ok()) {
        Some(value) => Some(value),
        None => {
            log::warn!("无法解析次级运动配置: {}", path.display());
            None
        }
    }
}

fn profile_map(config: Option<&Value>) -> HashMap<ProfileKind, Profile> {
    let values = config.and_then(|v| v.get("profiles"));
    [ProfileKind::Hair, ProfileKind::Tail, ProfileKind::Ear, ProfileKind::Cloth,
        ProfileKind::SoftBody, ProfileKind::Accessory]
        .into_iter().map(|kind| {
            let value = values.and_then(|v| v.get(kind.key()));
            (kind, kind.defaults().apply_json(value))
        }).collect()
}

fn collect_auto_chains(
    bones: &BoneManager,
    classified: &[Option<ProfileKind>],
    output: &mut Vec<(Vec<usize>, ProfileKind)>,
) {
    for index in 0..classified.len() {
        let Some(kind) = classified[index] else { continue; };
        let parent = bones.get_bone(index).and_then(|bone| bone.parent_id());
        let parent_same = parent.and_then(|p| classified.get(p)).copied().flatten().is_some();
        let parent_branching = parent.map(|p| {
            bones.children_of(p).iter().filter(|&&child| classified.get(child)
                .copied().flatten().is_some()).count() > 1
        }).unwrap_or(false);
        if parent_same && !parent_branching { continue; }
        let child_count = bones.children_of(index).iter().filter(|&&child| classified.get(child)
            .copied().flatten().is_some()).count();
        // A shared organizer bone such as "hair" must not be driven once per
        // branch. Its children become independent roots instead.
        if child_count > 1 { continue; }
        collect_classified_paths(bones, classified, index, kind, Vec::new(), output);
    }
}

fn collect_classified_paths(
    bones: &BoneManager,
    classified: &[Option<ProfileKind>],
    index: usize,
    kind: ProfileKind,
    mut path: Vec<usize>,
    output: &mut Vec<(Vec<usize>, ProfileKind)>,
) {
    path.push(index);
    let children = bones.children_of(index).iter().copied()
        .filter(|&child| classified.get(child).copied().flatten().is_some()).collect::<Vec<_>>();
    if children.is_empty() || children.len() > 1 {
        output.push((path, kind));
    } else if let Some(child) = children.first().copied() {
        collect_classified_paths(bones, classified, child, kind, path, output);
    }
}

fn collect_explicit_chains(
    bones: &BoneManager,
    config: Option<&Value>,
    output: &mut Vec<(Vec<usize>, ProfileKind)>,
) {
    let Some(chains) = config.and_then(|v| v.get("chains")).and_then(Value::as_array) else { return; };
    for entry in chains {
        let Some(root_name) = entry.get("root").and_then(Value::as_str) else { continue; };
        let Some(root) = bones.links().position(|bone| bone.name.eq_ignore_ascii_case(root_name)) else {
            log::warn!("secondary_motion.json 未找到骨骼: {}", root_name);
            continue;
        };
        let kind = parse_kind(entry.get("profile").and_then(Value::as_str).unwrap_or("accessory"));
        collect_all_paths(bones, root, Vec::new(), kind, output);
    }
}

fn collect_all_paths(
    bones: &BoneManager,
    index: usize,
    mut path: Vec<usize>,
    kind: ProfileKind,
    output: &mut Vec<(Vec<usize>, ProfileKind)>,
) {
    path.push(index);
    let children = bones.children_of(index);
    if children.is_empty() {
        output.push((path, kind));
    } else {
        for &child in children { collect_all_paths(bones, child, path.clone(), kind, output); }
    }
}

fn classify(name: &str) -> Option<ProfileKind> {
    if matches_any(name, &["hair".into(), "髪".into(), "发".into(), "髮".into(), "ヘア".into()]) {
        Some(ProfileKind::Hair)
    } else if matches_any(name, &["tail".into(), "尾".into()]) {
        Some(ProfileKind::Tail)
    } else if matches_any(name, &["ear".into(), "耳".into()]) {
        Some(ProfileKind::Ear)
    } else if matches_any(name, &["cloth".into(), "skirt".into(), "cape".into(), "dress".into(), "袖".into(), "裾".into(), "スカート".into()]) {
        Some(ProfileKind::Cloth)
    } else if matches_any(name, &["breast".into(), "bust".into(), "chestsoft".into(), "胸".into()]) {
        Some(ProfileKind::SoftBody)
    } else if matches_any(name, &["ribbon".into(), "リボン".into(), "phys".into(), "spring".into(), "jiggle".into(), "accessory".into()]) {
        Some(ProfileKind::Accessory)
    } else { None }
}

fn parse_kind(value: &str) -> ProfileKind {
    match value.to_ascii_lowercase().as_str() {
        "hair" => ProfileKind::Hair, "tail" => ProfileKind::Tail, "ear" => ProfileKind::Ear,
        "cloth" => ProfileKind::Cloth, "softbody" | "soft_body" => ProfileKind::SoftBody,
        _ => ProfileKind::Accessory,
    }
}

fn normalize(value: &str) -> String { value.trim().to_lowercase() }
fn matches_any(name: &str, markers: &[String]) -> bool {
    markers.iter().any(|marker| !marker.is_empty() && name.contains(&marker.to_lowercase()))
}
fn string_array(value: Option<&Value>) -> Vec<String> {
    value.and_then(Value::as_array).map(|values| values.iter()
        .filter_map(Value::as_str).map(normalize).collect()).unwrap_or_default()
}
fn number(value: &Value, key: &str, fallback: f32) -> f32 {
    value.get(key).and_then(Value::as_f64).map(|v| v as f32)
        .filter(|v| v.is_finite()).unwrap_or(fallback)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::skeleton::BoneLink;

    fn add_bone(bones: &mut BoneManager, name: &str, parent: i32, y: f32) {
        let mut bone = BoneLink::new(name.to_string());
        bone.parent_index = parent;
        bone.initial_position = glam::Vec3::new(0.0, y, 0.0);
        bones.add_bone(bone);
    }

    #[test]
    fn branching_organizer_is_not_solved_by_every_hair_chain() {
        let mut bones = BoneManager::new();
        add_bone(&mut bones, "hair", -1, 0.0);
        add_bone(&mut bones, "front_hair", 0, 1.0);
        add_bone(&mut bones, "front_hair_end", 1, 2.0);
        add_bone(&mut bones, "sidehair_L", 0, 1.0);
        add_bone(&mut bones, "sidehair_L_end", 3, 2.0);
        bones.build_hierarchy();

        let build = build_secondary_motion(&bones, Path::new("missing/model.pmx")).unwrap();
        assert_eq!(build.chain_count, 2);
        assert_eq!(build.joint_count, 4);
        assert!(build.data.springs.iter().all(|spring| spring.joints[0].node != 0));
    }

    #[test]
    fn unrelated_humanoid_bones_are_not_auto_detected() {
        let mut bones = BoneManager::new();
        add_bone(&mut bones, "hips", -1, 0.0);
        add_bone(&mut bones, "spine", 0, 1.0);
        add_bone(&mut bones, "head", 1, 2.0);
        bones.build_hierarchy();
        assert!(build_secondary_motion(&bones, Path::new("missing/model.pmx")).is_none());
    }

    #[test]
    fn bundled_model_has_secondary_motion_chains() {
        let model_path = Path::new("../src/main/resources/assets/openmmdchanged/mmd/mmd_latex/model.pmx");
        if !model_path.is_file() {
            return;
        }
        let model = crate::model::load_pmx(model_path).expect("bundled PMX should load");
        let build = build_secondary_motion(&model.bone_manager, model_path)
            .expect("bundled PMX should expose secondary-motion chains");
        println!("bundled secondary motion: {} chains / {} joints", build.chain_count, build.joint_count);
        assert!(build.chain_count >= 1);
        assert!(build.joint_count >= 2);
    }

    #[test]
    fn bundled_model_secondary_motion_changes_gpu_matrices() {
        let model_path = Path::new("../src/main/resources/assets/openmmdchanged/mmd/mmd_latex/model.pmx");
        if !model_path.is_file() {
            return;
        }
        let mut model = crate::model::load_pmx(model_path).expect("bundled PMX should load");
        assert!(model.init_secondary_motion(model_path));
        model.tick_animation_no_skinning(1.0 / 60.0);
        let before = model.bone_manager.get_skinning_matrices().to_vec();
        for _ in 0..30 {
            model.tick_animation_no_skinning(1.0 / 60.0);
        }
        let changed = before.iter().zip(model.bone_manager.get_skinning_matrices())
            .filter(|(a, b)| a.to_cols_array().iter().zip(b.to_cols_array())
                .any(|(x, y)| (x - y).abs() > 1.0e-5))
            .count();
        println!("bundled secondary motion changed {} bone matrices", changed);
        assert!(changed >= 2, "secondary motion must reach GPU skinning matrices");
    }
}
