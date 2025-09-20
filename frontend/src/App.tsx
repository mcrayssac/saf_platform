import { Link, Route, Routes } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"

function HomePage() {
    return (
        <Card className="max-w-3xl mx-auto">
            <CardContent className="p-6">
                <h1 className="text-2xl font-semibold mb-2">SAF Dashboard</h1>
                <p className="text-muted-foreground mb-4">
                    Bienvenue ! Utilisez la navigation pour gérer vos agents, messages et supervision.
                </p>
                <div className="flex gap-2">
                    <Button asChild><Link to="/agents">Agents</Link></Button>
                    <Button variant="secondary" asChild><Link to="/messaging">Messaging</Link></Button>
                    <Button variant="outline" asChild><Link to="/supervision">Supervision</Link></Button>
                </div>
            </CardContent>
        </Card>
    )
}

function AgentsPage() {
    return <div className="container py-8">
        <h2 className="text-xl font-semibold mb-4">Agents</h2>
        <p className="text-muted-foreground">Liste, création/suppression, état en temps réel (à venir).</p>
    </div>
}

function MessagingPage() {
    return <div className="container py-8">
        <h2 className="text-xl font-semibold mb-4">Messaging</h2>
        <p className="text-muted-foreground">Envoyer des messages / ask avec timeout (à venir).</p>
    </div>
}

function SupervisionPage() {
    return <div className="container py-8">
        <h2 className="text-xl font-semibold mb-4">Supervision</h2>
        <p className="text-muted-foreground">Redémarrages, métriques, journaux (à venir).</p>
    </div>
}

export default function App() {
    return (
        <div className="min-h-dvh">
            <header className="border-b">
                <div className="container flex h-14 items-center justify-between">
                    <Link to="/" className="font-semibold">SAF</Link>
                    <nav className="flex gap-6 text-sm">
                        <Link to="/agents" className="hover:underline">Agents</Link>
                        <Link to="/messaging" className="hover:underline">Messaging</Link>
                        <Link to="/supervision" className="hover:underline">Supervision</Link>
                    </nav>
                </div>
            </header>

            <main className="py-6">
                <Routes>
                    <Route path="/" element={<HomePage />} />
                    <Route path="/agents" element={<AgentsPage />} />
                    <Route path="/messaging" element={<MessagingPage />} />
                    <Route path="/supervision" element={<SupervisionPage />} />
                </Routes>
            </main>
        </div>
    )
}
