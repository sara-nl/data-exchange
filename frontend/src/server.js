import sirv from 'sirv'
import polka from 'polka'
import compression from 'compression'
import proxy from 'http-proxy-middleware'
import * as sapper from '@sapper/server'

const { PORT, NODE_ENV, BACKEND_BASE_URI } = process.env
const dev = NODE_ENV === 'development'
const backendBaseUri = BACKEND_BASE_URI || 'http://localhost:8000'

console.log(`Backend base URI: ${backendBaseUri}`)
console.log(`Dev mode: ${dev}`)

// Handle SIGTERM, to immediately quit when Docker asks us to.
process.on('SIGTERM', () => process.exit(128 + 15))

let server = polka()

if (dev) {
  server = server.use(
    proxy('/api', { target: backendBaseUri }),
    proxy('/static', { target: backendBaseUri }),
    proxy('/admin', { target: backendBaseUri })
  )
}

server
  .use(
    compression({ threshold: 0 }),
    sirv('static', { dev }),
    sapper.middleware()
  )
  .listen(PORT, (err) => {
    if (err) console.log('error', err)
  })
