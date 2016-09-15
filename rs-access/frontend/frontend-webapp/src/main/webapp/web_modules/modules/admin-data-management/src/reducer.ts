import DatasetFormReducer from "./dataset/model/datasetCreation.form.reducers"
import DatasetReducer from "./dataset/model/dataset.reducers"
import DatasetModelReducer from "./datasetmodel/model/model.reducer"
import ConnectionReducer from "./connection/model/connection.reducers"
import DatasourceReducer from "./datasource/model/datasource.reducers"
import { combineReducers } from "redux"

export const dataManagementReducer = combineReducers({
  DatasetFormReducer,
  DatasetReducer,
  DatasetModelReducer,
  ConnectionReducer,
  DatasourceReducer
})

