import { PlainRoute } from "react-router"
import DatasetCreateContainer from "./dataset/containers/DatasetCreateContainer"
import DatasetListContainer from "./dataset/containers/DatasetListContainer"
import DatamanagementContainer from "./containers/DatamanagementContainer"
import CollectionCreateContainer from "./collection/containers/CollectionCreateContainer"
import CollectionListContainer from "./collection/containers/CollectionListContainer"

export const datamanagementRoute: PlainRoute = {
  path: 'datamanagement',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: DatamanagementContainer
      })
    })
  }
}

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
