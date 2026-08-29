//! VRChat-humanoid 风格的骨骼角色识别。
//!
//! 不依赖固定命名：把任意常见骨名（MMD／VRM／Mixamo／Unreal Mannequin／英文／带 `左肩P` 这类
//! 变体）归一化后映射到「人形角色」。这样动画（如 Mixamo 人形 FBX）即使映射出的骨名与模型实际
//! 骨名不完全一致，也能按角色找到模型里真正的骨（例如动画给 `左肩`，模型实际叫 `左肩P`）。

/// 人形骨骼角色（对角色的规范枚举）
#[derive(Clone, Copy, PartialEq, Eq, Debug, Hash)]
pub enum HumanoidRole {
    Root,
    Hips,
    Spine,
    Chest,
    Neck,
    Head,
    LeftShoulder,
    LeftUpperArm,
    LeftLowerArm,
    LeftHand,
    RightShoulder,
    RightUpperArm,
    RightLowerArm,
    RightHand,
    LeftUpperLeg,
    LeftLowerLeg,
    LeftFoot,
    LeftToe,
    RightUpperLeg,
    RightLowerLeg,
    RightFoot,
    RightToe,
}

/// 归一化骨名：转小写、去掉常见前缀与分隔符（空格/下划线/连字符/点/冒号/斜杠）。
fn normalize(name: &str) -> String {
    let mut s = name.to_lowercase();
    for prefix in [
        "mixamorig:",
        "mixamorig_",
        "mixamorig",
        "mixamo:",
        "mixamo_",
        "unreal:",
        "unreal_",
        "unreal",
    ] {
        if let Some(stripped) = s.strip_prefix(prefix) {
            s = stripped.to_string();
            break;
        }
    }
    s.retain(|c| !matches!(c, ' ' | '_' | '-' | '.' | ':' | '/' | '\\'));
    s
}

/// 把骨名映射到人形角色（无法识别返回 None）。
pub fn role_of_name(name: &str) -> Option<HumanoidRole> {
    use HumanoidRole::*;
    let n = normalize(name);
    match n.as_str() {
        // 根
        "全ての親" | "root" | "allparent" | "全体" => Some(Root),
        // 髋/臀
        "センター" | "center" | "hips" | "pelvis" | "hip" | "下半身" => Some(Hips),
        // 脊柱
        "上半身" | "spine" | "spine01" | "spine1" | "lowerspine" => Some(Spine),
        // 胸
        "上半身2" | "spine2" | "spine02" | "chest" | "upperchest" | "spine3" | "spine03" => Some(Chest),
        // 颈
        "首" | "neck" | "neck01" | "neck1" | "cou" => Some(Neck),
        // 头
        "頭" | "head" | "head01" => Some(Head),

        // 左肩
        "左肩" | "左肩p" | "leftshoulder" | "shoulder_l" | "shoulderl"
        | "clavicle_l" | "claviclel" | "leftclavicle" => Some(LeftShoulder),
        // 左上臂
        "左腕" | "leftarm" | "leftupperarm" | "upperarm_l" | "upperarml" | "leftupperarm_l"
        | "leftx" | "leftuparm" => Some(LeftUpperArm),
        // 左前臂
        "左ひじ" | "左肘" | "leftforearm" | "leftlowerarm" | "lowerarm_l" | "lowerarml"
        | "leftelbow" | "elbow_l" | "elbowl" => Some(LeftLowerArm),
        // 左手
        "左手首" | "lefthand" | "hand_l" | "handl" | "leftwrist" | "wrist_l" | "wristl" => Some(LeftHand),

        // 右肩
        "右肩" | "右肩p" | "rightshoulder" | "shoulder_r" | "shoulderr" | "clavicle_r"
        | "clavicler" | "rightclavicle" => Some(RightShoulder),
        // 右上臂
        "右腕" | "rightarm" | "rightupperarm" | "upperarm_r" | "upperarmr" | "rightuparm" => Some(RightUpperArm),
        // 右前臂
        "右ひじ" | "右肘" | "rightforearm" | "rightlowerarm" | "lowerarm_r" | "lowerarmr"
        | "rightelbow" | "elbow_r" | "elbowr" => Some(RightLowerArm),
        // 右手
        "右手首" | "righthand" | "hand_r" | "handr" | "rightwrist" | "wrist_r" | "wristr" => Some(RightHand),

        // 左大腿
        "左足" | "leftupleg" | "leftupperleg" | "thigh_l" | "thighl" | "leftthigh" | "upperleg_l" => Some(LeftUpperLeg),
        // 左小腿
        "左ひざ" | "左膝" | "leftlowerleg" | "calf_l" | "calfl" | "leftshin" | "lowerleg_l" => Some(LeftLowerLeg),
        // 左脚
        "左足首" | "leftfoot" | "foot_l" | "footl" | "leftankle" | "ankle_l" | "anklel" => Some(LeftFoot),
        // 左脚趾
        "左つま先" | "左つまさき" | "lefttoebase" | "lefttoe" | "ball_l" | "balll" | "lefttoes" => Some(LeftToe),

        // 右大腿
        "右足" | "rightupleg" | "rightupperleg" | "thigh_r" | "thighr" | "rightthigh" | "upperleg_r" => Some(RightUpperLeg),
        // 右小腿
        "右ひざ" | "右膝" | "rightlowerleg" | "calf_r" | "calfr" | "rightshin" | "lowerleg_r" => Some(RightLowerLeg),
        // 右脚
        "右足首" | "rightfoot" | "foot_r" | "footr" | "rightankle" | "ankle_r" | "ankler" => Some(RightFoot),
        // 右脚趾
        "右つま先" | "右つまさき" | "righttoebase" | "righttoe" | "ball_r" | "ballr" | "righttoes" => Some(RightToe),

        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn recognizes_variants() {
        assert_eq!(role_of_name("左肩"), Some(HumanoidRole::LeftShoulder));
        assert_eq!(role_of_name("左肩P"), Some(HumanoidRole::LeftShoulder));
        assert_eq!(role_of_name("LeftShoulder"), Some(HumanoidRole::LeftShoulder));
        assert_eq!(role_of_name("mixamorig:LeftShoulder"), Some(HumanoidRole::LeftShoulder));
        assert_eq!(role_of_name("clavicle_l"), Some(HumanoidRole::LeftShoulder));
        assert_eq!(role_of_name("右腕"), Some(HumanoidRole::RightUpperArm));
        assert_eq!(role_of_name("upperarm_r"), Some(HumanoidRole::RightUpperArm));
    }

    #[test]
    fn recognizes_limbs() {
        assert_eq!(role_of_name("左手首"), Some(HumanoidRole::LeftHand));
        assert_eq!(role_of_name("LeftHand"), Some(HumanoidRole::LeftHand));
        assert_eq!(role_of_name("左ひじ"), Some(HumanoidRole::LeftLowerArm));
        assert_eq!(role_of_name("左足"), Some(HumanoidRole::LeftUpperLeg));
        assert_eq!(role_of_name("LeftThigh"), Some(HumanoidRole::LeftUpperLeg));
        assert_eq!(role_of_name("foot_l"), Some(HumanoidRole::LeftFoot));
        assert_eq!(role_of_name("頭"), Some(HumanoidRole::Head));
    }

    #[test]
    fn unknown_is_none() {
        assert_eq!(role_of_name("__unknown_bone__"), None);
    }
}
