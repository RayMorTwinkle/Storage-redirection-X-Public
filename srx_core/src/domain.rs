#[derive(Clone, Debug, PartialEq, Eq, Default)]
pub enum RedirectMode {
    #[default]
    Whitelist,
    Blacklist,
}

impl RedirectMode {
    pub fn from_str(value: &str) -> Self {
        match value.trim().to_ascii_lowercase().as_str() {
            "black" | "blacklist" => Self::Blacklist,
            _ => Self::Whitelist,
        }
    }

    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Whitelist => "whitelist",
            Self::Blacklist => "blacklist",
        }
    }

    pub fn is_blacklist(&self) -> bool {
        matches!(self, Self::Blacklist)
    }
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct PathMapping {
    pub request_path: String,
    pub final_path: String,
}

impl PathMapping {
    pub fn new(request_path: String, final_path: String) -> Self {
        Self {
            request_path,
            final_path,
        }
    }
}
