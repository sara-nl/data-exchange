module.exports = {
    root: true,
    parser: '@typescript-eslint/parser',
    plugins: [
      '@typescript-eslint',
    ],
    extends: [
      'eslint:recommended',
      'plugin:@typescript-eslint/eslint-recommended',
      'plugin:@typescript-eslint/recommended',
    ],
    rules: {
        "semi": "off",
        "@typescript-eslint/member-delimiter-style" : ['warn', {
            "multiline": {
                "delimiter": "comma",
                "requireLast": false
            },
            "singleline": {
                "delimiter": "semi",
                "requireLast": false
            },
            "overrides": {
                "interface": {
                    "multiline": {
                        "delimiter": "comma",
                        "requireLast": false
                    }
                }
            }
        }]
    }
  };