peg! parser_definition(r#"
"#);

macro_rules! vec {
    ( $( $x:expr ),* ) => {
        {
            let mut temp_vec = Vec::new();
            $(
                temp_vec.push($x);
            )*
            temp_vec
        }
    };
}

macro_rules! comments {
    () => {
        /// doc comment
        mod foo() {
            /** doc comment 2 */
            fn bar() {}
        }
    };
}

macro_rules! default {
    ($ty: ty) => { /* ANYTHING */ };
}

macro_rules! foobar {
    ($self: ident) => {  };
}

default!(String);

thread_local!(static HANDLE: Handle = Handle(0));

#[cfg(foo)]
foo!();

include!("path/to/rust/file.rs");
const STR: &str = include_str!("foo.in");
const BYTES: &[u8] = include_bytes!("data.data",);

include!(concat!(env!("OUT_DIR"), "/bindings.rs"));

std::include!("path/to/rust/file.rs");
::std::include!("path/to/rust/file.rs");
crate::foo! {}
self::foo! {}
super::foo! {}

fn foo() {
    #[cfg(foo)]
    foo! {}
    let a = 0; // needed to check that we parsed the call as a stmt

    macro_rules! bar {
        () => {};
    }

    let mut macro_rules = 0;
    macro_rules += 1;

    foo!() + foo!();


    // -- vec macro ---
    let v1 = vec![1, 2, 3];
    let v2 = vec![1; 10];
    let v: Vec<i32> = vec![];
    let vv: Vec<i32> = std::vec![]; // fully qualified macro call
    let vvv: Vec<i32> = std::vec /*comment*/ ![]; // fully qualified macro call with comment
    vec!(Foo[]); // custom vec macro
    // ----------------

    // --- format macros ---
    println!("{}", 92);
    format!("{argument}", argument = "test");  // => "test"
    format_args!("{name} {}", 1, name = 2);    // => "2 1"
    format!["hello {}", "world!"];
    format! {
        "x = {}, y = {y}",
        10, y = 30
    }
    panic!("division by zero");
    unimplemented!("{} {} {}", 1, 2, 3);
    todo!("it's too {epithet} to implement", epithet = "boring");
    std::println!("{}", 92); // fully qualified macro call
    std::println /*comment*/ !("{}", 92); // fully qualified macro call with comment
    ::std::println!("{}", 92); // fully qualified macro call beginning with double colon
    eprintln!(Foo[]); // custom format macro
    // -------------------

    // --- expr macros ---
    dbg!();
    dbg!("Some text");
    dbg!(123 + 567,);
    std::dbg!(123); // fully qualified macro call
    std::dbg /*comment*/ !(123); // fully qualified macro call with comment
    dbg!(Foo[]); // custom expr macro
    // ------------------

    // --- log macros ---
    error!();
    debug!("{a} {c} {b}", a="a", b='b', c=3);  // => "a 3 b"
    trace!(target: "smbc", "open_with {:?}", options);
    log::warn!(target: "smbc", "open_with {:?}", options); // fully qualified macro call
    log::info /*comment*/ !(target: "smbc", "open_with {:?}", options); // fully qualified macro call with comment
    debug!(log, "debug values"; "x" => 1, "y" => -1); // custom log macro
    // ------------------

    // --- assert macros ---
    let a = 42u32;
    let b = 43u32;
    assert!(a == b);
    assert![a == b];
    assert!{a == b};

    assert_eq!(a, b, "Some text");
    assert_ne!(a, b, "Some text");
    assert!(a == b, "Some text");
    assert!(a == b, "Text {} {} syntax", "with", "format");

    assert!(a == b);
    debug_assert!(a == b);
    assert_eq!(a, b);
    debug_assert_eq!(a, b);
    assert_ne!(a, b);
    debug_assert_ne!(a, b);
    std::assert!(a == b); // fully qualified macro call
    std::assert /*comment*/ !(a == b); // fully qualified macro call with comment
    assert_eq!(Foo[]); // custom assert macro
    // ---------------------

    // --- concat macros
    concat!("abc");
    concat!("abc", "def");
    concat!("abc", "def",);
    std::concat!("abc", "def"); // fully qualified macro call
    std::concat /*comment*/ !("abc", "def"); // fully qualified macro call with comment
    concat!(Foo[]); // custom concat macro
    // ------------------

    // - env macros
    env!("FOO");
    env!("FOO",);
    env!("FOO", "error message");
    env!("FOO", "error message", );
    std::env!("FOO"); // fully qualified macro call
    std::env /*comment*/ !("FOO"); // fully qualified macro call with comment
    env!(Foo[]); // custom env macro
    // ------------------

    // - asm macros
    asm!("nop");
    asm!("nop", "nop");
    asm!("nop", options(pure, nomem, nostack));
    asm!("nop", const 5, a = const 5);
    asm!("nop", sym foo::bar, a = sym foo::bar, const 6);
    asm!("nop", a = const A + 1);
    asm!("nop", in(reg) x => y, out("eax") _);
    asm!("nop", a = const 5, b = sym foo::bar, c = in(reg) _, d = out(reg) a => _);
    std::asm!("nop"); // fully qualified macro call
    std::asm /*comment*/ !("nop"); // fully qualified macro call with comment
    // ------------------
}
