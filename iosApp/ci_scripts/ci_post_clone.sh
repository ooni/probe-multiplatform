#!/bin/sh

# Fail this script if any subcommand fails.
set -e

HOMEBREW_NO_AUTO_UPDATE=1 # disable homebrew's automatic updates.

# Install CocoaPods using Homebrew.
brew install cocoapods

brew install ruby@3.3

brew install getsentry/tools/sentry-cli

echo 'export PATH="/usr/local/opt/ruby@3.3/bin:$PATH"' >> ~/.zshrc

source ~/.zshrc

export PATH="/usr/local/opt/ruby@3.3/bin:$PATH"

### Start fastlane setup
cd $CI_PRIMARY_REPOSITORY_PATH # change working directory to the root of your cloned repo.

echo 'export GEM_HOME=$HOME/gems' >>~/.zshrc
echo 'export PATH=$HOME/gems/bin:$PATH' >>~/.zshrc
export GEM_HOME=$HOME/gems
export PATH="$GEM_HOME/bin:$PATH"

gem install bundler --install-dir $GEM_HOME

ruby_arch_flag=$(ruby -e 'c = RbConfig::CONFIG; f = c["ARCH_FLAG"].to_s.strip; f = "-arch #{c["host_cpu"]}" if f.empty? && c["host_cpu"] != "universal"; print f')
export CONFIGURE_ARGS="--with-arch-flag='${ruby_arch_flag}'"
echo "Building native gems with ARCH_FLAG=${ruby_arch_flag} for $(ruby -v)"

if ! bundle install; then
    echo "\nbundle install failed - dumping native extension build logs"
    find "$GEM_HOME" -name mkmf.log -exec sh -c 'echo "\n----- $1"; cat "$1"' _ {} \;
    exit 1
fi

### End fastlane setup

### Start Java setup
root_dir=$CI_WORKSPACE_PATH
repo_dir=$CI_PRIMARY_REPOSITORY_PATH
jdk_dir="${CI_DERIVED_DATA_PATH}/JDK"

gradle_dir="${repo_dir}/Common"
cache_dir="${CI_DERIVED_DATA_PATH}/.gradle"

jdk_version="20.0.1"

# Check if we stored gradle caches in DerivedData.
recover_cache_files() {

    echo "\nRecover cache files"

    if [ ! -d $cache_dir ]; then
        echo " - No valid caches found, skipping"
        return 0
    fi

    echo " - Copying gradle cache to ${gradle_dir}"
    rm -rf "${gradle_dir}/.gradle"
    cp -r $cache_dir $gradle_dir

    return 0
}

# Install the JDK
install_jdk_if_needed() {

    echo "\nInstall JDK if needed"

    if [[ $(uname -m) == "arm64" ]]; then
        echo " - Detected M1"
        arch_type="macos-aarch64"
    else
        echo " - Detected Intel"
        arch_type="macos-x64"
    fi

    # Location of version / arch detection file.
    detect_loc="${jdk_dir}/.${jdk_version}.${arch_type}"

    if [ -f $detect_loc ]; then
        echo " - Found a valid JDK installation, skipping install"
        return 0
    fi

    echo " - No valid JDK installation found, installing..."

    tar_name="jdk-${jdk_version}_${arch_type}_bin.tar.gz"

    # Download and un-tar JDK to our defined location.
    curl -OL "https://download.oracle.com/java/20/archive/${tar_name}"
    tar xzf $tar_name -C $root_dir

    # Move the JDK to our desired location.
    rm -rf $jdk_dir
    mkdir -p $jdk_dir
    mv "${root_dir}/jdk-${jdk_version}.jdk/Contents/Home" $jdk_dir

    # Some cleanup.
    rm -r "${root_dir}/jdk-${jdk_version}.jdk"
    rm $tar_name

    # Add the detection file for subsequent builds.
    touch $detect_loc

    echo " - Set JAVA_HOME in Xcode Cloud to ${jdk_dir}/Home"

    return 0
}

recover_cache_files
install_jdk_if_needed

export JAVA_HOME="${jdk_dir}/Home"
### End Java setup

# The default execution directory of this script is the ci_scripts directory.
cd $CI_PRIMARY_REPOSITORY_PATH # change working directory to the root of your cloned repo.

echo "organization=${ORGANIZATION}" >> gradle.properties

./gradlew copyBrandingToCommonResources -Porganization=$ORGANIZATION

./gradlew podInstall -Porganization=$ORGANIZATION

# Install CocoaPods dependencies.
cd iosApp && pod install # run `pod install` in the `ios` directory.
