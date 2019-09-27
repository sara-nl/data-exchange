import resolve from 'rollup-plugin-node-resolve';
import replace from 'rollup-plugin-replace';
import commonjs from 'rollup-plugin-commonjs';
import svelte from 'rollup-plugin-svelte';
import babel from 'rollup-plugin-babel';
import { terser } from 'rollup-plugin-terser';
import config from 'sapper/config/rollup.js';
import pkg from './package.json';
import autoPreprocess from 'svelte-preprocess';
import typescript from 'rollup-plugin-typescript2';

const mode = process.env.NODE_ENV;
const dev = mode === 'development';
const legacy = !!process.env.SAPPER_LEGACY_BUILD;

const onwarn = (warning, onwarn) => (warning.code === 'CIRCULAR_DEPENDENCY' && /[/\\]@sapper[/\\]/.test(warning.message)) || onwarn(warning);
const dedupe = importee => importee === 'svelte' || importee.startsWith('svelte/');

export default {
    client: {
        input: config.client.input(),
        output: config.client.output(),
        plugins: [
            typescript({
                typescript: require('typescript')
            }),
            replace({
                'process.browser': true,
                'process.env.NODE_ENV': JSON.stringify(mode)
            }),
            svelte({
                dev,
                hydratable: true,
                emitCss: true,
                preprocess: autoPreprocess(),
            }),
            resolve({
                browser: true,
                extensions: [ '.mjs', '.js', '.ts', '.svelte', '.json', '.node' ],
                dedupe
            }),
            commonjs(),

            legacy && babel({
                extensions: ['.js', '.ts', '.mjs', '.html', '.svelte'],
                runtimeHelpers: true,
                exclude: ['node_modules/@babel/**'],
                presets: [
                    ['@babel/preset-env', {
                        targets: '> 0.25%, not dead'
                    }],
                    "@babel/preset-typescript",
                ],
                plugins: [
                    '@babel/plugin-syntax-dynamic-import',
                    ['@babel/plugin-transform-runtime', {
                        useESModules: true
                    }]
                ]
            }),

            !dev && terser({
                module: true
            })
        ],

        onwarn,
    },

    server: {
        input: config.server.input(),
        output: config.server.output(),
        plugins: [
            typescript({
                typescript: require('typescript')
            }),
            replace({
                'process.browser': false,
                'process.env.NODE_ENV': JSON.stringify(mode)
            }),
            svelte({
                generate: 'ssr',
                dev,
                preprocess: autoPreprocess(),
            }),
            resolve({
                extensions: [ '.mjs', '.js', '.ts', '.svelte', '.json', '.node' ],
                dedupe
            }),
            commonjs()
        ],
        external: Object.keys(pkg.dependencies).concat(
            require('module').builtinModules || Object.keys(process.binding('natives'))
        ),

        onwarn(warning, next) {
            if (warning.code === "THIS_IS_UNDEFINED") {
                return;
            }
            next(warning);
        },
    },

    serviceworker: {
        input: config.serviceworker.input(),
        output: config.serviceworker.output(),
        plugins: [
            resolve(),
            replace({
                'process.browser': true,
                'process.env.NODE_ENV': JSON.stringify(mode)
            }),
            commonjs(),
            !dev && terser()
        ],

        onwarn,
    },


};
