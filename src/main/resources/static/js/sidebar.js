/* Toggle behavior for the reusable Thymeleaf sidebar. No dependencies required. */
(() => {
    "use strict";

    const STORAGE_KEY = "cms-sidebar-collapsed";

    function readSavedState() {
        try {
            return window.localStorage.getItem(STORAGE_KEY) === "true";
        } catch (error) {
            // Sidebar still works when browser storage is unavailable.
            return false;
        }
    }

    function saveState(collapsed) {
        try {
            window.localStorage.setItem(STORAGE_KEY, String(collapsed));
        } catch (error) {
            // Ignore storage restrictions; the current page remains interactive.
        }
    }

    function applyState(sidebar, collapsed) {
        sidebar.classList.toggle("is-collapsed", collapsed);

        const button = sidebar.querySelector("[data-sidebar-toggle]");
        if (button) {
            const label = collapsed ? "Mở rộng thanh bên" : "Thu gọn thanh bên";
            button.setAttribute("aria-expanded", String(!collapsed));
            button.setAttribute("aria-label", label);
            button.setAttribute("title", label);
        }

        sidebar.querySelectorAll(".sidebar-link").forEach((link) => {
            if (collapsed) {
                link.setAttribute("title", link.dataset.sidebarLabel || "Điều hướng");
            } else {
                link.removeAttribute("title");
            }
        });
    }

    document.querySelectorAll(".app-sidebar").forEach((sidebar) => {
        applyState(sidebar, readSavedState());

        const button = sidebar.querySelector("[data-sidebar-toggle]");
        if (!button) return;

        button.addEventListener("click", () => {
            const collapsed = !sidebar.classList.contains("is-collapsed");
            applyState(sidebar, collapsed);
            saveState(collapsed);
        });
    });
})();
