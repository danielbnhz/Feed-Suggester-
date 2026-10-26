export type EventType = "CLICK" | "MORE_LIKE_THIS";

export function logEvent(articleId: number, type: EventType): void {
    fetch("/api/events", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ articleId, type }),
        keepalive: true,
    }).catch(() => {

    });
}