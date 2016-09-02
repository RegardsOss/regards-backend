import { ModelAttribute } from "../datasetmodel/ModelAttribute"

// Add a new datasetmodel
export const ADD_DATASOURCE_MODEL = 'ADD_DATASOURCE_MODEL'

export const addDatasourceModel = (name: string, attributes: Array<ModelAttribute>) => ({
  type: ADD_DATASOURCE_MODEL,
  entity: {
    id: Math.floor(Math.random() * 60) + 10,
    attributes,
    name
  }
})
