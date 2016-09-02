import { PlainRoute } from "react-router"
import DatasetCreateContainer from "./dataset/containers/DatasetCreateContainer"
import DatasetListContainer from "./dataset/containers/DatasetListContainer"
import DatamanagementContainer from "./containers/DatamanagementContainer"
import CollectionCreateContainer from "./collection/containers/CollectionCreateContainer"
import CollectionListContainer from "./collection/containers/CollectionListContainer"
import DatasourceCreateContainer from "./datasource/containers/DatasourceCreateContainer"
import DatasourceListContainer from "./datasource/containers/DatasourceListContainer"
import ConnectionCreateContainer from "./connection/containers/ConnectionCreateContainer"
import ConnectionListContainer from "./connection/containers/ConnectionListContainer"
import DatasetModelCreateContainer from "./datasetmodel/containers/DatasetModelCreateContainer"
import DatasetModelListContainer from "./datasetmodel/containers/DatasetModelListContainer"
import { DatasourceModelListContainer } from "./datasourcemodel/containers/DatasourceModelListContainer"
import { DatasourceModelCreateContainer } from "./datasourcemodel/containers/DatasourceModelCreateContainer"

export const datasetListRoute: PlainRoute = {
  path: 'datamanagement/dataset',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: DatasetListContainer
      })
    })
  }
}

export const datasetCreateRoute: PlainRoute = {
  path: 'datamanagement/dataset/create',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: DatasetCreateContainer
      })
    })
  }
}


export const collectionListRoute: PlainRoute = {
  path: 'datamanagement/collection',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: CollectionListContainer
      })
    })
  }
}

export const collectionCreateRoute: PlainRoute = {
  path: 'datamanagement/collection/create',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: CollectionCreateContainer
      })
    })
  }
}


export const datasetModelCreateRoute: PlainRoute = {
  path: 'datamanagement/datasetmodel/create(/:from)',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: DatasetModelCreateContainer
      })
    })
  }
}

export const datasetModelListRoute: PlainRoute = {
  path: 'datamanagement/datasetmodel',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: DatasetModelListContainer
      })
    })
  }
}

export const datasourceCreateRoute: PlainRoute = {
  path: 'datamanagement/datasource/create(/:from)',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: DatasourceCreateContainer
      })
    })
  }
}

export const datasourceListRoute: PlainRoute = {
  path: 'datamanagement/datasource',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: DatasourceListContainer
      })
    })
  }
}

export const datasourceModelCreateRoute: PlainRoute = {
  path: 'datamanagement/datasourcemodel/create(/:from)',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: DatasourceModelCreateContainer
      })
    })
  }
}

export const datasourceModelListRoute: PlainRoute = {
  path: 'datamanagement/datasourcemodel',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: DatasourceModelListContainer
      })
    })
  }
}

export const connectionCreateRoute: PlainRoute = {
  path: 'datamanagement/connection/create(/:from)',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: ConnectionCreateContainer
      })
    })
  }
}

export const connectionListRoute: PlainRoute = {
  path: 'datamanagement/connection',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: ConnectionListContainer
      })
    })
  }
}


export const datamanagementhome: PlainRoute = {
  path: 'datamanagement',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: DatamanagementContainer
      })
    })
  }
}

export const datamanagementRouter: PlainRoute = {
  path: '',
  childRoutes: [
    datamanagementhome,
    collectionCreateRoute,
    collectionListRoute,
    datasetListRoute,
    datasetCreateRoute,
    datasetModelCreateRoute,
    datasetModelListRoute,
    datasourceCreateRoute,
    datasourceListRoute,
    datasourceModelCreateRoute,
    datasourceModelListRoute,
    connectionCreateRoute,
    connectionListRoute
  ]
}
