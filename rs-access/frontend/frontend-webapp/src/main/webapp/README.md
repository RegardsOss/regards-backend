# REGARDS Access frontend

This is the REGARDS Access frontend.

## Install

```
chmod +x ./scripts/bootstrap.sh
npm run boostrap
npm install
npm run test
```

## Build

```
npm run build:production
```

## Dependencies

- node v6
- npm v3
- webpack v1.13
- babel

## Architecture

See rs-docs/front/architecture.md


## Plugins


All plugins in plugins/ directory have to be React components.
See the maven project frontend-plugins to see plugin exemple HelloWorldPlugin
