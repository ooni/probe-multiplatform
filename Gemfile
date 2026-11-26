source "https://rubygems.org"

gem "fastlane", "~> 2.233.0"
gem 'abbrev'
gem 'logger'
gem 'mutex_m'
gem 'csv'

plugins_path = File.join(File.dirname(__FILE__), 'fastlane', 'Pluginfile')
eval_gemfile(plugins_path) if File.exist?(plugins_path)
