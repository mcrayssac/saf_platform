import { ThemeToggle } from "@/components/theme-toggle"
import { useTheme } from "@/context/theme-provider"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card"
import {
    NavigationMenu,
    NavigationMenuIndicator,
    NavigationMenuItem,
    NavigationMenuLink,
    NavigationMenuList,
    NavigationMenuViewport,
} from "@/components/ui/navigation-menu"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Separator } from "@/components/ui/separator"

const navigationLinkClass =
    "group inline-flex h-9 w-max items-center justify-center rounded-md bg-transparent px-4 py-2 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground focus:bg-accent focus:text-accent-foreground focus:outline-none disabled:pointer-events-none disabled:opacity-50"

const sections = [
    {
        id: "agents",
        title: "Agents",
        headline: "Gestion centralisée des agents",
        description:
            "Suivez l'état global de vos agents, déclenchez des synchronisations et préparez les mises à jour.",
        highlights: [
            "Vue synthétique des agents et de leur statut",
            "Ouverture rapide de fiches détaillées",
            "Pipeline de provisionning en préparation",
        ],
        status: "En cours",
    },
    {
        id: "messaging",
        title: "Messaging",
        headline: "Communication fiable",
        description:
            "Planifiez des envois, suivez les confirmations et automatisez les réponses selon le contexte.",
        highlights: [
            "Gestion des files de messages et retours",
            "Ask avec timeout configurable",
            "Modèles conversationnels en bêta",
        ],
        status: "Bientôt",
    },
    {
        id: "supervision",
        title: "Supervision",
        headline: "Pilotage en production",
        description:
            "Consultez les métriques clés, déclenchez des redémarrages ciblés et accédez aux journaux centralisés.",
        highlights: [
            "Dashboards temps-réel",
            "Alerting unifié (à venir)",
            "Exports pour observabilité externe",
        ],
        status: "Planifié",
    },
]

const quickActions = [
    {
        id: "launch-agents",
        label: "Ouvrir la console Agents",
        description: "Configurer les agents et ajuster leurs paramètres en direct.",
        href: "#agents",
        variant: "default" as const,
    },
    {
        id: "compose-message",
        label: "Nouvelle campagne",
        description: "Préparer une diffusion et gérer ses retours facilement.",
        href: "#messaging",
        variant: "secondary" as const,
    },
    {
        id: "view-supervision",
        label: "Tableau de bord supervision",
        description: "Visualiser la santé globale et l'historique des incidents.",
        href: "#supervision",
        variant: "outline" as const,
    },
]

export default function App() {
    const { resolvedTheme } = useTheme()

    return (
        <div className="min-h-dvh bg-background text-foreground">
            <div className="mx-auto flex min-h-dvh w-full max-w-6xl flex-col gap-10 px-4 py-10">
                <Card className="border-border/80">
                    <CardHeader className="flex flex-col gap-6 sm:flex-row sm:items-start sm:justify-between">
                        <div className="space-y-2">
                            <Badge variant="outline" className="w-max uppercase tracking-wide">
                                SAF Platform
                            </Badge>
                            <CardTitle className="text-3xl tracking-tight">Console de supervision</CardTitle>
                            <CardDescription>
                                Pilotez agents, messagerie et supervision dans une interface cohérente, optimisée
                                pour les modes clair et sombre.
                            </CardDescription>
                        </div>
                        <CardContent className="flex w-full flex-col items-start gap-3 p-0 sm:w-auto sm:items-end">
                            <Badge variant="secondary" className="w-max">
                                Thème actif&nbsp;: {resolvedTheme === "dark" ? "Sombre" : "Clair"}
                            </Badge>
                            <ThemeToggle />
                        </CardContent>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <NavigationMenu className="justify-start">
                            <NavigationMenuList>
                                {sections.map((section) => (
                                    <NavigationMenuItem key={section.id}>
                                        <NavigationMenuLink asChild>
                                            <a className={navigationLinkClass} href={`#${section.id}`}>
                                                {section.title}
                                            </a>
                                        </NavigationMenuLink>
                                    </NavigationMenuItem>
                                ))}
                            </NavigationMenuList>
                            <NavigationMenuIndicator />
                            <NavigationMenuViewport />
                        </NavigationMenu>
                        <Separator />
                        <div className="grid gap-4 md:grid-cols-3">
                            {quickActions.map((action) => (
                                <Card key={action.id} className="border-dashed">
                                    <CardHeader className="space-y-3">
                                        <CardTitle className="text-base font-semibold">
                                            {action.label}
                                        </CardTitle>
                                        <CardDescription>{action.description}</CardDescription>
                                    </CardHeader>
                                    <CardFooter>
                                        <Button asChild variant={action.variant} className="w-full">
                                            <a href={action.href}>Accéder</a>
                                        </Button>
                                    </CardFooter>
                                </Card>
                            ))}
                        </div>
                    </CardContent>
                </Card>

                <ScrollArea className="max-h-[560px] rounded-xl border border-border/80">
                    <div className="grid gap-4 p-6 md:grid-cols-3" role="region" aria-labelledby="sections">
                        {sections.map((section) => (
                            <Card key={section.id} id={section.id} className="h-full">
                                <CardHeader className="space-y-1">
                                    <div className="flex items-center justify-between gap-2">
                                        <CardTitle className="text-lg font-semibold">
                                            {section.title}
                                        </CardTitle>
                                        <Badge variant="outline">{section.status}</Badge>
                                    </div>
                                    <CardDescription>{section.headline}</CardDescription>
                                </CardHeader>
                                <CardContent className="space-y-4">
                                    <p className="text-sm text-muted-foreground">
                                        {section.description}
                                    </p>
                                    <Separator />
                                    <ul className="space-y-2 text-sm">
                                        {section.highlights.map((highlight) => (
                                            <li key={highlight} className="flex items-start gap-2">
                                                <span className="mt-1 h-1.5 w-1.5 rounded-full bg-primary" aria-hidden />
                                                <span>{highlight}</span>
                                            </li>
                                        ))}
                                    </ul>
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                </ScrollArea>
            </div>
        </div>
    )
}
