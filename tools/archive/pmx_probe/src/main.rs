use mmd::{DefaultConfig, Error};
use mmd::pmx::reader::*;
use std::{env, fs::File, io::BufReader};
use std::io::Write;

fn readers(path: &str) -> Result<VertexReader<BufReader<File>>, Error> {
    let header = HeaderReader::new(BufReader::new(File::open(path)?))?;
    VertexReader::new(header)
}

fn main() -> Result<(), Error> {
    let path = env::args().nth(1).unwrap();
    let output = env::args().nth(2);
    let mut vr = readers(&path)?;
    let vertices = vr.iter::<DefaultConfig>().collect::<Result<Vec<_>, _>>()?;

    let mut sr = SurfaceReader::new(readers(&path)?)?;
    let surfaces = sr.iter::<DefaultConfig>().collect::<Result<Vec<_>, _>>()?;

    let sr = SurfaceReader::new(readers(&path)?)?;
    let tr = TextureReader::new(sr)?;
    let mut mr = MaterialReader::new(tr)?;
    let materials = mr.iter::<DefaultConfig>().collect::<Result<Vec<_>, _>>()?;

    let mut tri_start = 0usize;
    for (material_id, material) in materials.iter().enumerate() {
        let tri_count = material.surface_count as usize / 3;
        if material.local_name.to_lowercase().contains("eye") {
            let tris = &surfaces[tri_start..tri_start + tri_count];
            let mut uv_min = [f32::INFINITY; 2];
            let mut uv_max = [f32::NEG_INFINITY; 2];
            let mut pos_min = [f32::INFINITY; 3];
            let mut pos_max = [f32::NEG_INFINITY; 3];
            let mut uv_samples = Vec::new();
            for tri in tris {
                let mut center = [0.0f32; 2];
                for &index in tri {
                    let v = &vertices[index as usize];
                    for j in 0..2 {
                        uv_min[j] = uv_min[j].min(v.uv[j]);
                        uv_max[j] = uv_max[j].max(v.uv[j]);
                        center[j] += v.uv[j] / 3.0;
                    }
                    for j in 0..3 {
                        pos_min[j] = pos_min[j].min(v.position[j]);
                        pos_max[j] = pos_max[j].max(v.position[j]);
                    }
                }
                if uv_samples.len() < 24 { uv_samples.push(center); }
            }
            println!("material={} id={} triangles={} texture={} uv_min={:?} uv_max={:?} pos_min={:?} pos_max={:?}",
                material.local_name, material_id, tri_count, material.texture_index,
                uv_min, uv_max, pos_min, pos_max);
            println!("uv_centers={:?}", uv_samples);
            if let Some(output_path) = &output {
                let mut file = File::create(output_path)?;
                writeln!(file, "x0,y0,z0,u0,v0,x1,y1,z1,u1,v1,x2,y2,z2,u2,v2").unwrap();
                for tri in tris {
                    let mut values = Vec::new();
                    for &index in tri {
                        let vertex = &vertices[index as usize];
                        values.extend_from_slice(&[
                            vertex.position[0], vertex.position[1], vertex.position[2],
                            vertex.uv[0], vertex.uv[1]
                        ]);
                    }
                    writeln!(file, "{}", values.iter().map(|v| v.to_string()).collect::<Vec<_>>().join(",")).unwrap();
                }
            }
        }
        tri_start += tri_count;
    }
    Ok(())
}
