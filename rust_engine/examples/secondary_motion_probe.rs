use mmd_engine::model::load_pmx;

fn main() {
    let path = std::env::args().nth(1).expect("PMX path is required");
    let mut model = load_pmx(path).expect("PMX load failed");
    assert!(model.init_physics(), "no secondary physics was initialized");
    let tail = model.bone_manager.find_bone_by_name("tail.001")
        .expect("tail.001 bone is required for this probe");
    model.set_model_position_and_yaw(0.0, 0.0, 0.0, 0.0);
    for _ in 0..4 { model.tick_animation(1.0 / 60.0); }
    let before = model.bone_manager.get_global_transform(tail);
    model.set_model_position_and_yaw(0.12, 0.0, 0.0, 0.35);
    model.tick_animation(1.0 / 60.0);
    let after = model.bone_manager.get_global_transform(tail);
    let delta = before.to_cols_array().iter().zip(after.to_cols_array())
        .map(|(a, b)| (a - b).abs()).sum::<f32>();
    println!("secondary_motion_matrix_delta={delta:.6}");
    assert!(delta > 0.001, "secondary bone matrix did not react to entity motion");
}
