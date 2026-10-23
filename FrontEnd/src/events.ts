export type EventType = "CLICK";

export function logEvent(articleId: number, type: EventType): void {
    fetch("/api/events", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ articleId, type }),
        keepalive: true,
    }).catch(() => {

    });
}