/// JNI bindings for BoringTun library
use std::os::raw::c_char;
use std::ptr;

use jni::objects::{JByteBuffer, JClass, JString};
use jni::strings::JNIStr;
use jni::sys::{jbyteArray, jint, jlong, jshort, jstring};
use jni::JNIEnv;
use parking_lot::Mutex;

use boringtun::ffi::new_tunnel;
use boringtun::ffi::wireguard_read;
use boringtun::ffi::wireguard_result;
use boringtun::ffi::wireguard_tick;
use boringtun::ffi::wireguard_write;
use boringtun::ffi::x25519_key;
use boringtun::ffi::x25519_key_to_base64;
use boringtun::ffi::x25519_key_to_hex;
use boringtun::ffi::x25519_public_key;
use boringtun::ffi::x25519_secret_key;

use boringtun::noise::Tunn;

pub extern "C" fn log_print(_log_string: *const c_char) {
    /*
    XXX:
    Define callback function in app.
    */
}

/// demo one of calling rust code via JNI
#[no_mangle]
#[export_name = "Java_jni_OONIProbeEngineJNI_demoOne"]
pub extern "C" fn demo_one(env: JNIEnv, _class: JClass) -> jstring {
    match env.byte_array_from_slice(&x25519_secret_key().key) {
        Ok(v) => v,
        Err(_) => ptr::null_mut(),
    }
    return env.new_string("one")
}

/// demo two
#[no_mangle]
#[export_name = "Java_jni_OONIProbeEngineJNI_demoTwo"]
pub unsafe extern "C" fn generate_public_key1(
    env: JNIEnv,
    _class: JClass,
) -> jbyteArray {
    match env.byte_array_from_slice(&x25519_secret_key().key) {
        Ok(v) => v,
        Err(_) => ptr::null_mut(),
    }
    return env.new_string("two")
}