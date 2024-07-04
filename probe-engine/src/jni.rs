/// JNI bindings for probe-engine
use std::os::raw::c_char;
use std::ptr;

use jni::objects::{JByteBuffer, JClass, JString};
use jni::strings::JNIStr;
use jni::sys::jstring;
use jni::JNIEnv;

/// demo one of calling rust code via JNI
#[no_mangle]
#[export_name = "Java_jni_OONIProbeEngineJNI_demoOne"]
pub extern "system" fn demo_one(env: JNIEnv, _class: JClass) -> jstring {
    let output = env.new_string(format!("one")).expect("cannot make string");
    output.into_raw()
}

#[no_mangle]
#[export_name = "Java_jni_OONIProbeEngineJNI_demoTwo"]
pub extern "system" fn demo_two(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let output = env.new_string("two").expect("unable to make string");
    output.into_raw()
}
