use rust2go::{GoCompiler, LinkType};
use std::{env, path::Path, process::Command};

fn main() {
    rust2go::Builder::new()
        .with_link(LinkType::Dynamic)
        .with_go_compiler(CrossGoCompiler {})
        .with_go_src("./go")
        .build();
}

#[derive(Debug, Clone, Copy)]
pub struct CrossGoCompiler;

impl GoCompiler for CrossGoCompiler {
    fn go_build(&self, go_src: &Path, link: LinkType, output: &Path) {
        let mut go_build = Command::new("go");

        for (key, value) in env::vars() {
            println!("OOVARS: {}: {}", key, value);
        }

        let cargo_target_arch = env::var("CARGO_CFG_TARGET_ARCH").unwrap();
        println!("cargo_target_arch: {}", cargo_target_arch);
        let goarch = match cargo_target_arch.as_str() {
            "arm" => "arm",
            "arm64" => "arm64",
            "aarch64" => "arm64",
            "x86" => "386",
            "x86_64" => "amd64",
            _ => "unknown",
        };

        let go_build_cc = env::var("RUST_ANDROID_GRADLE_CC").unwrap();
        let go_ldflags = env::var("RUST_ANDROID_GRADLE_CC_LINK_ARG").unwrap();

        go_build
            .env("CGO_ENABLED", "1")
            .env("GOOS", "android")
            .env("CC", go_build_cc.as_str())
            .env("GOARCH", goarch)
            .env("LDFLAGS", go_ldflags.as_str())
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
