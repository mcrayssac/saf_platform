import {
    createContext,
    useContext,
    useEffect,
    useMemo,
    useState,
    type ReactNode,
} from "react"

export type Theme = "light" | "dark" | "system"

interface ThemeContextValue {
    theme: Theme
    resolvedTheme: "light" | "dark"
    setTheme: (theme: Theme) => void
}

const ThemeProviderContext = createContext<ThemeContextValue | undefined>(undefined)

const prefersDark = () => window.matchMedia("(prefers-color-scheme: dark)").matches

export function ThemeProvider({
    children,
    defaultTheme = "system",
    storageKey = "app-theme",
}: {
    children: ReactNode
    defaultTheme?: Theme
    storageKey?: string
}) {
    const [theme, setTheme] = useState<Theme>(() => {
        if (typeof window === "undefined") return defaultTheme
        const stored = window.localStorage.getItem(storageKey) as Theme | null
        return stored ?? defaultTheme
    })

    const [resolvedTheme, setResolvedTheme] = useState<"light" | "dark">(() => {
        if (typeof window === "undefined") return "light"
        if (defaultTheme === "system") return prefersDark() ? "dark" : "light"
        return defaultTheme
    })

    useEffect(() => {
        if (typeof window === "undefined") return

        const root = window.document.documentElement
        const media = window.matchMedia("(prefers-color-scheme: dark)")

        const apply = () => {
            const nextResolved = theme === "system" ? (media.matches ? "dark" : "light") : theme
            setResolvedTheme(nextResolved)
            root.classList.toggle("dark", nextResolved === "dark")
            root.setAttribute("data-theme", nextResolved)

            if (theme === "system") {
                window.localStorage.removeItem(storageKey)
            } else {
                window.localStorage.setItem(storageKey, theme)
            }
        }

        apply()

        const listener = () => {
            if (theme === "system") {
                apply()
            }
        }

        media.addEventListener("change", listener)
        return () => media.removeEventListener("change", listener)
    }, [theme, storageKey])

    const value = useMemo(() => ({ theme, resolvedTheme, setTheme }), [theme, resolvedTheme])

    return <ThemeProviderContext.Provider value={value}>{children}</ThemeProviderContext.Provider>
}

export function useTheme() {
    const context = useContext(ThemeProviderContext)
    if (!context) {
        throw new Error("useTheme must be used within a ThemeProvider")
    }
    return context
}
