/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion.lint

val CLIPPY_LINTS: List<Lint> = listOf(
    Lint("absurd_extreme_comparisons", false),
    Lint("all", true),
    Lint("alloc_instead_of_core", false),
    Lint("allow_attributes_without_reason", false),
    Lint("almost_complete_letter_range", false),
    Lint("almost_swapped", false),
    Lint("approx_constant", false),
    Lint("arithmetic", false),
    Lint("as_conversions", false),
    Lint("as_underscore", false),
    Lint("assertions_on_constants", false),
    Lint("assertions_on_result_states", false),
    Lint("assign_op_pattern", false),
    Lint("assign_ops", false),
    Lint("async_yields_async", false),
    Lint("await_holding_invalid_type", false),
    Lint("await_holding_lock", false),
    Lint("await_holding_refcell_ref", false),
    Lint("bad_bit_mask", false),
    Lint("bind_instead_of_map", false),
    Lint("blacklisted_name", false),
    Lint("blanket_clippy_restriction_lints", false),
    Lint("blocks_in_if_conditions", false),
    Lint("bool_assert_comparison", false),
    Lint("bool_comparison", false),
    Lint("borrow_as_ptr", false),
    Lint("borrow_deref_ref", false),
    Lint("borrow_interior_mutable_const", false),
    Lint("borrowed_box", false),
    Lint("box_collection", false),
    Lint("boxed_local", false),
    Lint("branches_sharing_code", false),
    Lint("builtin_type_shadow", false),
    Lint("bytes_count_to_len", false),
    Lint("bytes_nth", false),
    Lint("cargo", true),
    Lint("cargo_common_metadata", false),
    Lint("case_sensitive_file_extension_comparisons", false),
    Lint("cast_abs_to_unsigned", false),
    Lint("cast_enum_constructor", false),
    Lint("cast_enum_truncation", false),
    Lint("cast_lossless", false),
    Lint("cast_possible_truncation", false),
    Lint("cast_possible_wrap", false),
    Lint("cast_precision_loss", false),
    Lint("cast_ptr_alignment", false),
    Lint("cast_ref_to_mut", false),
    Lint("cast_sign_loss", false),
    Lint("cast_slice_different_sizes", false),
    Lint("cast_slice_from_raw_parts", false),
    Lint("char_lit_as_u8", false),
    Lint("chars_last_cmp", false),
    Lint("chars_next_cmp", false),
    Lint("checked_conversions", false),
    Lint("clone_double_ref", false),
    Lint("clone_on_copy", false),
    Lint("clone_on_ref_ptr", false),
    Lint("cloned_instead_of_copied", false),
    Lint("cmp_nan", false),
    Lint("cmp_null", false),
    Lint("cmp_owned", false),
    Lint("cognitive_complexity", false),
    Lint("collapsible_else_if", false),
    Lint("collapsible_if", false),
    Lint("collapsible_match", false),
    Lint("collapsible_str_replace", false),
    Lint("comparison_chain", false),
    Lint("comparison_to_empty", false),
    Lint("complexity", true),
    Lint("copy_iterator", false),
    Lint("correctness", true),
    Lint("crate_in_macro_def", false),
    Lint("create_dir", false),
    Lint("crosspointer_transmute", false),
    Lint("dbg_macro", false),
    Lint("debug_assert_with_mut_call", false),
    Lint("decimal_literal_representation", false),
    Lint("declare_interior_mutable_const", false),
    Lint("default_instead_of_iter_empty", false),
    Lint("default_numeric_fallback", false),
    Lint("default_trait_access", false),
    Lint("default_union_representation", false),
    Lint("deprecated", true),
    Lint("deprecated_cfg_attr", false),
    Lint("deprecated_semver", false),
    Lint("deref_addrof", false),
    Lint("deref_by_slicing", false),
    Lint("derivable_impls", false),
    Lint("derive_hash_xor_eq", false),
    Lint("derive_ord_xor_partial_ord", false),
    Lint("derive_partial_eq_without_eq", false),
    Lint("disallowed_methods", false),
    Lint("disallowed_names", false),
    Lint("disallowed_script_idents", false),
    Lint("disallowed_types", false),
    Lint("diverging_sub_expression", false),
    Lint("doc_link_with_quotes", false),
    Lint("doc_markdown", false),
    Lint("double_comparisons", false),
    Lint("double_must_use", false),
    Lint("double_neg", false),
    Lint("double_parens", false),
    Lint("drop_copy", false),
    Lint("drop_non_drop", false),
    Lint("drop_ref", false),
    Lint("duplicate_mod", false),
    Lint("duplicate_underscore_argument", false),
    Lint("duration_subsec", false),
    Lint("else_if_without_else", false),
    Lint("empty_drop", false),
    Lint("empty_enum", false),
    Lint("empty_line_after_outer_attr", false),
    Lint("empty_loop", false),
    Lint("empty_structs_with_brackets", false),
    Lint("enum_clike_unportable_variant", false),
    Lint("enum_glob_use", false),
    Lint("enum_variant_names", false),
    Lint("eq_op", false),
    Lint("equatable_if_let", false),
    Lint("erasing_op", false),
    Lint("err_expect", false),
    Lint("excessive_precision", false),
    Lint("exhaustive_enums", false),
    Lint("exhaustive_structs", false),
    Lint("exit", false),
    Lint("expect_fun_call", false),
    Lint("expect_used", false),
    Lint("expl_impl_clone_on_copy", false),
    Lint("explicit_auto_deref", false),
    Lint("explicit_counter_loop", false),
    Lint("explicit_deref_methods", false),
    Lint("explicit_into_iter_loop", false),
    Lint("explicit_iter_loop", false),
    Lint("explicit_write", false),
    Lint("extend_from_slice", false),
    Lint("extend_with_drain", false),
    Lint("extra_unused_lifetimes", false),
    Lint("fallible_impl_from", false),
    Lint("field_reassign_with_default", false),
    Lint("filetype_is_file", false),
    Lint("filter_map", false),
    Lint("filter_map_identity", false),
    Lint("filter_map_next", false),
    Lint("filter_next", false),
    Lint("find_map", false),
    Lint("flat_map_identity", false),
    Lint("flat_map_option", false),
    Lint("float_arithmetic", false),
    Lint("float_cmp", false),
    Lint("float_cmp_const", false),
    Lint("float_equality_without_abs", false),
    Lint("fn_address_comparisons", false),
    Lint("fn_params_excessive_bools", false),
    Lint("fn_to_numeric_cast", false),
    Lint("fn_to_numeric_cast_any", false),
    Lint("fn_to_numeric_cast_with_truncation", false),
    Lint("for_kv_map", false),
    Lint("for_loops_over_fallibles", false),
    Lint("forget_copy", false),
    Lint("forget_non_drop", false),
    Lint("forget_ref", false),
    Lint("format_in_format_args", false),
    Lint("format_push_string", false),
    Lint("from_iter_instead_of_collect", false),
    Lint("from_over_into", false),
    Lint("from_str_radix_10", false),
    Lint("future_not_send", false),
    Lint("get_first", false),
    Lint("get_last_with_len", false),
    Lint("get_unwrap", false),
    Lint("identity_op", false),
    Lint("if_let_mutex", false),
    Lint("if_let_redundant_pattern_matching", false),
    Lint("if_not_else", false),
    Lint("if_same_then_else", false),
    Lint("if_then_some_else_none", false),
    Lint("ifs_same_cond", false),
    Lint("implicit_clone", false),
    Lint("implicit_hasher", false),
    Lint("implicit_return", false),
    Lint("implicit_saturating_sub", false),
    Lint("imprecise_flops", false),
    Lint("inconsistent_digit_grouping", false),
    Lint("inconsistent_struct_constructor", false),
    Lint("index_refutable_slice", false),
    Lint("indexing_slicing", false),
    Lint("ineffective_bit_mask", false),
    Lint("inefficient_to_string", false),
    Lint("infallible_destructuring_match", false),
    Lint("infinite_iter", false),
    Lint("inherent_to_string", false),
    Lint("inherent_to_string_shadow_display", false),
    Lint("init_numbered_fields", false),
    Lint("inline_always", false),
    Lint("inline_asm_x86_att_syntax", false),
    Lint("inline_asm_x86_intel_syntax", false),
    Lint("inline_fn_without_body", false),
    Lint("inspect_for_each", false),
    Lint("int_plus_one", false),
    Lint("integer_arithmetic", false),
    Lint("integer_division", false),
    Lint("into_iter_on_ref", false),
    Lint("invalid_null_ptr_usage", false),
    Lint("invalid_regex", false),
    Lint("invalid_upcast_comparisons", false),
    Lint("invalid_utf8_in_unchecked", false),
    Lint("invisible_characters", false),
    Lint("is_digit_ascii_radix", false),
    Lint("items_after_statements", false),
    Lint("iter_cloned_collect", false),
    Lint("iter_count", false),
    Lint("iter_next_loop", false),
    Lint("iter_next_slice", false),
    Lint("iter_not_returning_iterator", false),
    Lint("iter_nth", false),
    Lint("iter_nth_zero", false),
    Lint("iter_on_empty_collections", false),
    Lint("iter_on_single_items", false),
    Lint("iter_overeager_cloned", false),
    Lint("iter_skip_next", false),
    Lint("iter_with_drain", false),
    Lint("iterator_step_by_zero", false),
    Lint("just_underscores_and_digits", false),
    Lint("large_const_arrays", false),
    Lint("large_digit_groups", false),
    Lint("large_enum_variant", false),
    Lint("large_include_file", false),
    Lint("large_stack_arrays", false),
    Lint("large_types_passed_by_value", false),
    Lint("len_without_is_empty", false),
    Lint("len_zero", false),
    Lint("let_and_return", false),
    Lint("let_underscore_drop", false),
    Lint("let_underscore_lock", false),
    Lint("let_underscore_must_use", false),
    Lint("let_unit_value", false),
    Lint("linkedlist", false),
    Lint("logic_bug", false),
    Lint("lossy_float_literal", false),
    Lint("macro_use_imports", false),
    Lint("main_recursion", false),
    Lint("manual_assert", false),
    Lint("manual_async_fn", false),
    Lint("manual_bits", false),
    Lint("manual_filter_map", false),
    Lint("manual_find", false),
    Lint("manual_find_map", false),
    Lint("manual_flatten", false),
    Lint("manual_instant_elapsed", false),
    Lint("manual_map", false),
    Lint("manual_memcpy", false),
    Lint("manual_non_exhaustive", false),
    Lint("manual_ok_or", false),
    Lint("manual_range_contains", false),
    Lint("manual_rem_euclid", false),
    Lint("manual_retain", false),
    Lint("manual_saturating_arithmetic", false),
    Lint("manual_split_once", false),
    Lint("manual_str_repeat", false),
    Lint("manual_string_new", false),
    Lint("manual_strip", false),
    Lint("manual_swap", false),
    Lint("manual_unwrap_or", false),
    Lint("many_single_char_names", false),
    Lint("map_clone", false),
    Lint("map_collect_result_unit", false),
    Lint("map_entry", false),
    Lint("map_err_ignore", false),
    Lint("map_flatten", false),
    Lint("map_identity", false),
    Lint("map_unwrap_or", false),
    Lint("match_as_ref", false),
    Lint("match_bool", false),
    Lint("match_like_matches_macro", false),
    Lint("match_on_vec_items", false),
    Lint("match_overlapping_arm", false),
    Lint("match_ref_pats", false),
    Lint("match_result_ok", false),
    Lint("match_same_arms", false),
    Lint("match_single_binding", false),
    Lint("match_str_case_mismatch", false),
    Lint("match_wild_err_arm", false),
    Lint("match_wildcard_for_single_variants", false),
    Lint("maybe_infinite_iter", false),
    Lint("mem_forget", false),
    Lint("mem_replace_option_with_none", false),
    Lint("mem_replace_with_default", false),
    Lint("mem_replace_with_uninit", false),
    Lint("min_max", false),
    Lint("misaligned_transmute", false),
    Lint("mismatched_target_os", false),
    Lint("mismatching_type_param_order", false),
    Lint("misrefactored_assign_op", false),
    Lint("missing_const_for_fn", false),
    Lint("missing_docs_in_private_items", false),
    Lint("missing_enforced_import_renames", false),
    Lint("missing_errors_doc", false),
    Lint("missing_inline_in_public_items", false),
    Lint("missing_panics_doc", false),
    Lint("missing_safety_doc", false),
    Lint("missing_spin_loop", false),
    Lint("mistyped_literal_suffixes", false),
    Lint("mixed_case_hex_literals", false),
    Lint("mixed_read_write_in_expression", false),
    Lint("mod_module_files", false),
    Lint("module_inception", false),
    Lint("module_name_repetitions", false),
    Lint("modulo_arithmetic", false),
    Lint("modulo_one", false),
    Lint("multi_assignments", false),
    Lint("multiple_crate_versions", false),
    Lint("multiple_inherent_impl", false),
    Lint("must_use_candidate", false),
    Lint("must_use_unit", false),
    Lint("mut_from_ref", false),
    Lint("mut_mut", false),
    Lint("mut_mutex_lock", false),
    Lint("mut_range_bound", false),
    Lint("mutable_key_type", false),
    Lint("mutex_atomic", false),
    Lint("mutex_integer", false),
    Lint("naive_bytecount", false),
    Lint("needless_arbitrary_self_type", false),
    Lint("needless_bitwise_bool", false),
    Lint("needless_bool", false),
    Lint("needless_borrow", false),
    Lint("needless_borrowed_reference", false),
    Lint("needless_collect", false),
    Lint("needless_continue", false),
    Lint("needless_doctest_main", false),
    Lint("needless_for_each", false),
    Lint("needless_late_init", false),
    Lint("needless_lifetimes", false),
    Lint("needless_match", false),
    Lint("needless_option_as_deref", false),
    Lint("needless_option_take", false),
    Lint("needless_parens_on_range_literals", false),
    Lint("needless_pass_by_value", false),
    Lint("needless_question_mark", false),
    Lint("needless_range_loop", false),
    Lint("needless_return", false),
    Lint("needless_splitn", false),
    Lint("needless_update", false),
    Lint("neg_cmp_op_on_partial_ord", false),
    Lint("neg_multiply", false),
    Lint("negative_feature_names", false),
    Lint("never_loop", false),
    Lint("new_ret_no_self", false),
    Lint("new_without_default", false),
    Lint("no_effect", false),
    Lint("no_effect_replace", false),
    Lint("no_effect_underscore_binding", false),
    Lint("non_ascii_literal", false),
    Lint("non_octal_unix_permissions", false),
    Lint("non_send_fields_in_send_ty", false),
    Lint("nonminimal_bool", false),
    Lint("nonsensical_open_options", false),
    Lint("nonstandard_macro_braces", false),
    Lint("not_unsafe_ptr_arg_deref", false),
    Lint("nursery", true),
    Lint("obfuscated_if_else", false),
    Lint("octal_escapes", false),
    Lint("ok_expect", false),
    Lint("only_used_in_recursion", false),
    Lint("op_ref", false),
    Lint("option_as_ref_deref", false),
    Lint("option_env_unwrap", false),
    Lint("option_filter_map", false),
    Lint("option_if_let_else", false),
    Lint("option_map_or_none", false),
    Lint("option_map_unit_fn", false),
    Lint("option_option", false),
    Lint("or_fun_call", false),
    Lint("or_then_unwrap", false),
    Lint("out_of_bounds_indexing", false),
    Lint("overflow_check_conditional", false),
    Lint("overly_complex_bool_expr", false),
    Lint("panic", false),
    Lint("panic_in_result_fn", false),
    Lint("panicking_unwrap", false),
    Lint("partialeq_ne_impl", false),
    Lint("partialeq_to_none", false),
    Lint("path_buf_push_overwrite", false),
    Lint("pattern_type_mismatch", false),
    Lint("pedantic", true),
    Lint("perf", true),
    Lint("positional_named_format_parameters", false),
    Lint("possible_missing_comma", false),
    Lint("precedence", false),
    Lint("print_in_format_impl", false),
    Lint("print_literal", false),
    Lint("print_stderr", false),
    Lint("print_stdout", false),
    Lint("print_with_newline", false),
    Lint("println_empty_string", false),
    Lint("ptr_arg", false),
    Lint("ptr_as_ptr", false),
    Lint("ptr_eq", false),
    Lint("ptr_offset_with_cast", false),
    Lint("pub_enum_variant_names", false),
    Lint("pub_use", false),
    Lint("question_mark", false),
    Lint("range_minus_one", false),
    Lint("range_plus_one", false),
    Lint("range_step_by_zero", false),
    Lint("range_zip_with_len", false),
    Lint("rc_buffer", false),
    Lint("rc_clone_in_vec_init", false),
    Lint("rc_mutex", false),
    Lint("read_zero_byte_vec", false),
    Lint("recursive_format_impl", false),
    Lint("redundant_allocation", false),
    Lint("redundant_clone", false),
    Lint("redundant_closure", false),
    Lint("redundant_closure_call", false),
    Lint("redundant_closure_for_method_calls", false),
    Lint("redundant_else", false),
    Lint("redundant_feature_names", false),
    Lint("redundant_field_names", false),
    Lint("redundant_pattern", false),
    Lint("redundant_pattern_matching", false),
    Lint("redundant_pub_crate", false),
    Lint("redundant_slicing", false),
    Lint("redundant_static_lifetimes", false),
    Lint("ref_binding_to_reference", false),
    Lint("ref_option_ref", false),
    Lint("regex_macro", false),
    Lint("repeat_once", false),
    Lint("replace_consts", false),
    Lint("rest_pat_in_fully_bound_structs", false),
    Lint("restriction", true),
    Lint("result_large_err", false),
    Lint("result_map_or_into_option", false),
    Lint("result_map_unit_fn", false),
    Lint("result_unit_err", false),
    Lint("return_self_not_must_use", false),
    Lint("reversed_empty_ranges", false),
    Lint("same_functions_in_if_condition", false),
    Lint("same_item_push", false),
    Lint("same_name_method", false),
    Lint("search_is_some", false),
    Lint("self_assignment", false),
    Lint("self_named_constructors", false),
    Lint("self_named_module_files", false),
    Lint("semicolon_if_nothing_returned", false),
    Lint("separated_literal_suffix", false),
    Lint("serde_api_misuse", false),
    Lint("shadow_reuse", false),
    Lint("shadow_same", false),
    Lint("shadow_unrelated", false),
    Lint("short_circuit_statement", false),
    Lint("should_assert_eq", false),
    Lint("should_implement_trait", false),
    Lint("significant_drop_in_scrutinee", false),
    Lint("similar_names", false),
    Lint("single_char_add_str", false),
    Lint("single_char_lifetime_names", false),
    Lint("single_char_pattern", false),
    Lint("single_component_path_imports", false),
    Lint("single_element_loop", false),
    Lint("single_match", false),
    Lint("single_match_else", false),
    Lint("size_of_in_element_count", false),
    Lint("skip_while_next", false),
    Lint("slow_vector_initialization", false),
    Lint("stable_sort_primitive", false),
    Lint("std_instead_of_alloc", false),
    Lint("std_instead_of_core", false),
    Lint("str_to_string", false),
    Lint("string_add", false),
    Lint("string_add_assign", false),
    Lint("string_extend_chars", false),
    Lint("string_from_utf8_as_bytes", false),
    Lint("string_lit_as_bytes", false),
    Lint("string_slice", false),
    Lint("string_to_string", false),
    Lint("strlen_on_c_strings", false),
    Lint("struct_excessive_bools", false),
    Lint("style", true),
    Lint("suboptimal_flops", false),
    Lint("suspicious", true),
    Lint("suspicious_arithmetic_impl", false),
    Lint("suspicious_assignment_formatting", false),
    Lint("suspicious_else_formatting", false),
    Lint("suspicious_map", false),
    Lint("suspicious_op_assign_impl", false),
    Lint("suspicious_operation_groupings", false),
    Lint("suspicious_splitn", false),
    Lint("suspicious_to_owned", false),
    Lint("suspicious_unary_op_formatting", false),
    Lint("swap_ptr_to_ref", false),
    Lint("tabs_in_doc_comments", false),
    Lint("temporary_assignment", false),
    Lint("to_digit_is_some", false),
    Lint("to_string_in_format_args", false),
    Lint("todo", false),
    Lint("too_many_arguments", false),
    Lint("too_many_lines", false),
    Lint("toplevel_ref_arg", false),
    Lint("trailing_empty_array", false),
    Lint("trait_duplication_in_bounds", false),
    Lint("transmute_bytes_to_str", false),
    Lint("transmute_float_to_int", false),
    Lint("transmute_int_to_bool", false),
    Lint("transmute_int_to_char", false),
    Lint("transmute_int_to_float", false),
    Lint("transmute_num_to_bytes", false),
    Lint("transmute_ptr_to_ptr", false),
    Lint("transmute_ptr_to_ref", false),
    Lint("transmute_undefined_repr", false),
    Lint("transmutes_expressible_as_ptr_casts", false),
    Lint("transmuting_null", false),
    Lint("trim_split_whitespace", false),
    Lint("trivial_regex", false),
    Lint("trivially_copy_pass_by_ref", false),
    Lint("try_err", false),
    Lint("type_complexity", false),
    Lint("type_repetition_in_bounds", false),
    Lint("undocumented_unsafe_blocks", false),
    Lint("undropped_manually_drops", false),
    Lint("unicode_not_nfc", false),
    Lint("unimplemented", false),
    Lint("uninit_assumed_init", false),
    Lint("uninit_vec", false),
    Lint("unit_arg", false),
    Lint("unit_cmp", false),
    Lint("unit_hash", false),
    Lint("unit_return_expecting_ord", false),
    Lint("unnecessary_cast", false),
    Lint("unnecessary_filter_map", false),
    Lint("unnecessary_find_map", false),
    Lint("unnecessary_fold", false),
    Lint("unnecessary_join", false),
    Lint("unnecessary_lazy_evaluations", false),
    Lint("unnecessary_mut_passed", false),
    Lint("unnecessary_operation", false),
    Lint("unnecessary_owned_empty_strings", false),
    Lint("unnecessary_self_imports", false),
    Lint("unnecessary_sort_by", false),
    Lint("unnecessary_to_owned", false),
    Lint("unnecessary_unwrap", false),
    Lint("unnecessary_wraps", false),
    Lint("unneeded_field_pattern", false),
    Lint("unneeded_wildcard_pattern", false),
    Lint("unnested_or_patterns", false),
    Lint("unreachable", false),
    Lint("unreadable_literal", false),
    Lint("unsafe_derive_deserialize", false),
    Lint("unsafe_removed_from_name", false),
    Lint("unsafe_vector_initialization", false),
    Lint("unseparated_literal_suffix", false),
    Lint("unsound_collection_transmute", false),
    Lint("unstable_as_mut_slice", false),
    Lint("unstable_as_slice", false),
    Lint("unused_async", false),
    Lint("unused_collect", false),
    Lint("unused_io_amount", false),
    Lint("unused_peekable", false),
    Lint("unused_rounding", false),
    Lint("unused_self", false),
    Lint("unused_unit", false),
    Lint("unusual_byte_groupings", false),
    Lint("unwrap_in_result", false),
    Lint("unwrap_or_else_default", false),
    Lint("unwrap_used", false),
    Lint("upper_case_acronyms", false),
    Lint("use_debug", false),
    Lint("use_self", false),
    Lint("used_underscore_binding", false),
    Lint("useless_asref", false),
    Lint("useless_attribute", false),
    Lint("useless_conversion", false),
    Lint("useless_format", false),
    Lint("useless_let_if_seq", false),
    Lint("useless_transmute", false),
    Lint("useless_vec", false),
    Lint("vec_box", false),
    Lint("vec_init_then_push", false),
    Lint("vec_resize_to_zero", false),
    Lint("verbose_bit_mask", false),
    Lint("verbose_file_reads", false),
    Lint("vtable_address_comparisons", false),
    Lint("while_immutable_condition", false),
    Lint("while_let_loop", false),
    Lint("while_let_on_iterator", false),
    Lint("wildcard_dependencies", false),
    Lint("wildcard_enum_match_arm", false),
    Lint("wildcard_imports", false),
    Lint("wildcard_in_or_patterns", false),
    Lint("write_literal", false),
    Lint("write_with_newline", false),
    Lint("writeln_empty_string", false),
    Lint("wrong_pub_self_convention", false),
    Lint("wrong_self_convention", false),
    Lint("wrong_transmute", false),
    Lint("zero_divided_by_zero", false),
    Lint("zero_prefixed_literal", false),
    Lint("zero_ptr", false),
    Lint("zero_sized_map_values", false),
    Lint("zst_offset", false)
)
