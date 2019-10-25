const webpack = require('webpack');
const path = require('path');
const config = require('sapper/config/webpack.js');
const preprocess = require('svelte-preprocess');
const pkg = require('./package.json');

const SpeedMeasurePlugin = require("speed-measure-webpack-plugin");
const smpConfig = {
    granularLoaderData: true,
};
const smpClient = new SpeedMeasurePlugin(smpConfig);
const smpServer = new SpeedMeasurePlugin(smpConfig);

const mode = process.env.NODE_ENV;
const dev = mode === 'development';

const alias = { svelte: path.resolve('node_modules', 'svelte') };
const extensions = ['.ts', '.mjs', '.js', '.json', '.svelte', '.html'];
const mainFields = ['svelte', 'module', 'browser', 'main'];

module.exports = {
    client: smpClient.wrap({
        entry: config.client.entry(),
        output: config.client.output(),
        resolve: { alias, extensions, mainFields },
        module: {
            rules: [
                {
                    test: /\.(svelte|html)$/,
                    use: {
                        loader: 'svelte-loader',
                        options: {
                            dev,
                            preprocess: preprocess(),
                            hydratable: true,
                            hotReload: true,
                        },
                    },
                },
                {
                    test: /\.tsx?$/,
                    use: 'ts-loader',
                    exclude: /node_modules/,
                },
            ],
        },
        mode,
        plugins: [
            dev && new webpack.HotModuleReplacementPlugin(),
            new webpack.DefinePlugin({
                'process.browser': true,
                'process.env.NODE_ENV': JSON.stringify(mode)
            }),
        ].filter(Boolean),
        devtool: dev && 'cheap-module-eval-source-map'
    }),

    server: smpServer.wrap({
        entry: config.server.entry(),
        output: config.server.output(),
        target: 'node',
        resolve: { alias, extensions, mainFields },
        externals: Object.keys(pkg.dependencies).concat('encoding'),
        module: {
            rules: [
                {
                    test: /\.(svelte|html)$/,
                    use: {
                        loader: 'svelte-loader',
                        options: {
                            css: false,
                            preprocess: preprocess(),
                            generate: 'ssr',
                            dev
                        }
                    }
                },
                {
                    test: /\.tsx?$/,
                    use: 'ts-loader',
                    exclude: /node_modules/,
                },
            ]
        },
        mode,
        performance: {
            hints: false // it doesn't matter if server.js is large
        }
    }),

    serviceworker: {
        entry: config.serviceworker.entry(),
        output: config.serviceworker.output(),
        mode,
    }
};
