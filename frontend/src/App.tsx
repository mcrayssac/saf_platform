import { Button } from "@/components/ui/button"

const sections = [
    {
        id: "agents",
        title: "Agents",
        description: "Liste, création/suppression, état en temps réel (à venir).",
    },
    {
        id: "messaging",
        title: "Messaging",
        description: "Envoyer des messages / ask avec timeout (à venir).",
    },
    {
        id: "supervision",
        title: "Supervision",
        description: "Redémarrages, métriques, journaux (à venir).",
    },
]

export default function App() {
    return (
        <div className="flex min-h-dvh flex-col bg-background text-foreground">
            <header className="border-b">
                <div className="mx-auto flex h-14 w-full max-w-5xl items-center justify-between px-4">
                    <span className="text-lg font-semibold tracking-tight">SAF</span>
                    <nav className="flex gap-6 text-sm text-muted-foreground">
                        {sections.map((section) => (
                            <a
                                key={section.id}
                                href={`#${section.id}`}
                                className="transition-colors hover:text-foreground"
                            >
                                {section.title}
                            </a>
                        ))}
                    </nav>
                </div>
            </header>

            <main className="flex flex-1 flex-col gap-10 px-4 py-12">
                <section className="mx-auto w-full max-w-3xl rounded-xl border bg-card p-8 text-card-foreground shadow-sm">
                    <div className="space-y-4">
                        <div>
                            <h1 className="text-3xl font-semibold tracking-tight">SAF Dashboard</h1>
                            <p className="text-muted-foreground">
                                Bienvenue ! Utilisez la navigation pour gérer vos agents, messages et supervision.
                            </p>
                        </div>
                        <div className="flex flex-wrap gap-3">
                            <Button asChild>
                                <a href="#agents">Agents</a>
                            </Button>
                            <Button variant="secondary" asChild>
                                <a href="#messaging">Messaging</a>
                            </Button>
                            <Button variant="outline" asChild>
                                <a href="#supervision">Supervision</a>
                            </Button>
                        </div>
                    </div>
                </section>

                <div className="mx-auto grid w-full max-w-5xl gap-6 md:grid-cols-3">
                    {sections.map((section) => (
                        <section
                            key={section.id}
                            id={section.id}
                            className="rounded-xl border bg-card p-6 text-card-foreground shadow-sm"
                        >
                            <h2 className="text-xl font-semibold">{section.title}</h2>
                            <p className="mt-2 text-sm text-muted-foreground">{section.description}</p>
                        </section>
                    ))}
                </div>
            </main>
        </div>
    )
}
