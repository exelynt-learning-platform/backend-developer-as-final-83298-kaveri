(function () {
    var STORAGE_KEY = "swagger-theme";

    function currentTheme() {
        var theme = document.documentElement.getAttribute("data-theme");
        return theme === "dark" ? "dark" : "light";
    }

    function apply(theme) {
        document.documentElement.setAttribute("data-theme", theme);
        var button = document.getElementById("swagger-theme-toggle");
        if (!button) {
            return;
        }
        var dark = theme === "dark";
        button.textContent = dark ? "Light mode" : "Dark mode";
        button.setAttribute("aria-pressed", dark ? "true" : "false");
        button.setAttribute("aria-label", dark ? "Switch to light mode" : "Switch to dark mode");
    }

    function mount() {
        if (document.getElementById("swagger-theme-toggle")) {
            apply(currentTheme());
            return;
        }
        var button = document.createElement("button");
        button.id = "swagger-theme-toggle";
        button.type = "button";
        button.addEventListener("click", function () {
            var next = currentTheme() === "dark" ? "light" : "dark";
            localStorage.setItem(STORAGE_KEY, next);
            apply(next);
        });
        function place() {
            var authorize = document.querySelector(".swagger-ui .auth-wrapper .authorize");
            if (!authorize || !authorize.parentElement) {
                return false;
            }
            authorize.parentElement.insertBefore(button, authorize);
            apply(currentTheme());
            return true;
        }

        if (!place()) {
            var observer = new MutationObserver(function () {
                if (place()) {
                    observer.disconnect();
                }
            });
            observer.observe(document.body, { childList: true, subtree: true });
        }
    }

    if (document.body) {
        mount();
    } else {
        document.addEventListener("DOMContentLoaded", mount);
    }
})();
