import { ModelAttribute } from "../datasetmodel/ModelAttribute"

// Add a new datasetmodel
export const ADD_DATASOURCE_MODEL = 'ADD_DATASOURCE_MODEL'

export const addDatasourceModel = (id: number, name: string, attributes: Array<ModelAttribute>) => ({
  type: ADD_DATASOURCE_MODEL,
  entity: {
    id,
    attributes,
    name
  }
})
