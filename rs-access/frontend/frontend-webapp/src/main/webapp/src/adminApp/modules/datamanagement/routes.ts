import { PlainRoute } from "react-router"
import DatasetCreateContainer from "./dataset/containers/DatasetCreateContainer"
import DatasetContainer from "./dataset/containers/DatasetContainer"
import DatamanagementContainer from "./containers/DatamanagementContainer"

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

export const datasetRoute: PlainRoute = {
  path: 'datamanagement/dataset',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: DatasetContainer
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
