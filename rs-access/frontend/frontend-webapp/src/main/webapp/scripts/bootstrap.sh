

## Compiles bottom to top
npm link web_modules/models
npm link web_modules/some-pckg
cd web_modules/api && npm link @regardsoss/models && cd ../..
npm link web_modules/api
npm link web_modules/access-rights
npm link web_modules/i18n
npm link web_modules/injector
npm link web_modules/endpoints
cd web_modules/authentification && npm link @regardsoss/endpoints && cd ../.. && npm link web_modules/authentification
npm link web_modules/theme
