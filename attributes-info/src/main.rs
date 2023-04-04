mod formatter;
mod compiler_features;
mod builtin_attributes;

const COMPILER_FEATURES_PATH: &str = "src/main/resources/compiler-info/compiler-features.json";
const BUILTIN_ATTRIBUTES_PATH: &str = "src/main/resources/compiler-info/builtin-attributes.json";

fn main() {
    compiler_features::update_compiler_features(COMPILER_FEATURES_PATH);
    builtin_attributes::generate_builtin_attributes_json(BUILTIN_ATTRIBUTES_PATH);
}
