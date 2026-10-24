// An active request owns its response; leaving or hiding the page aborts it.
export function createPoller(load, { interval = 8000, periodic = true, visible = () => !document.hidden, onError = () => {} } = {}) {
  let controller, timer, stopped = true
  async function tick() {
    if (stopped || controller || !visible()) return
    const request = new AbortController()
    controller = request
    try { await load(request.signal) }
    catch (error) { if (error.name !== 'AbortError') onError(error) }
    finally { if (controller === request) controller = undefined }
  }
  return {
    start() { if (!stopped) return; stopped = false; tick(); if (periodic) timer = setInterval(tick, interval) },
    refresh() { controller?.abort(); controller = undefined; return tick() },
    pause() { stopped = true; clearInterval(timer); controller?.abort() },
  }
}
