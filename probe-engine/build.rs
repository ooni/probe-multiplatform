use rust2go::{GoCompiler, LinkType};
use std::{
    env,
    path::{Path},
    process::Command,
};

fn main() {
    rust2go::Builder::new()
        .with_go_compiler(CrossGoCompiler {})
        .with_go_src("./go")
        .build();
}

#[derive(Debug, Clone, Copy)]
pub struct CrossGoCompiler;

impl GoCompiler for CrossGoCompiler {
    fn go_build(&self, go_src: &Path, link: LinkType, output: &Path) {
        let mut go_build = Command::new("go");

        let build_linker = env::var("RUSTC_LINKER").ok();
        if let Some(cc) = build_linker {
            go_build.env("CC", cc.clone());
        }

        go_build
            .env("GO111MODULE", "on")
            .current_dir(go_src)
            .arg("build")
            .arg(if link == LinkType::Static {
                "-buildmode=c-archive"
            } else {
                "-buildmode=c-shared"
            })
            .arg("-o")
            .arg(output)
            .arg(".");

        go_build.status().expect("Go build failed");
    }
}
