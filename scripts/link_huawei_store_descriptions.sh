#!/bin/bash
### Usage ###
# This script is used to link the android store descriptions to the huawei store descriptions
# This script should be run from the root of the project
# `./scripts/link_huawei_store_descriptions.sh `
### END ###
android_dir="fastlane/metadata/android"
huawei_dir="fastlane/metadata/huawei"

for dir in "$android_dir"/*; do
  dir_name=$(basename "$dir")

  cd $huawei_dir

  ln -s "../android/$dir_name" "./$dir_name"
  echo "Linked ../android/$dir_name to ./$dir_name"

    cd $dir_name

    ln -sf "./title.txt" "./app_name.txt"
    echo "Linked title.txt to app_name.txt"

    ln -sf "./short_description.txt" "./introduction.txt"
    echo "Linked short_description.txt to introduction.txt"

    ln -sf "./full_description.txt" "./app_description.txt"
    echo "Linked full_description.txt to app_description.txt"

    cd ..

done
