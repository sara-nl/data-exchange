import sirv from 'sirv';
import polka from 'polka';
import compression from 'compression';
import proxy from 'http-proxy-middleware';
import * as sapper from '@sapper/server';

const { PORT, NODE_ENV } = process.env;
const dev = NODE_ENV === 'development';

polka()
    .use(
        proxy('/api', {
          logLevel: 'debug',
          target: 'http://localhost:8000',
        }),
        proxy('/static', {
          logLevel: 'debug',
          target: 'http://localhost:8000',
        }),
        compression({ threshold: 0 }),
        sirv('static', { dev }),
        sapper.middleware()
    )
    .listen(PORT, err => {
        if (err) console.log('error', err);
    });
