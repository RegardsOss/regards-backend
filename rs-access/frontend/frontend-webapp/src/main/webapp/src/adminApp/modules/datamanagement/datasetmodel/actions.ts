import { ModelAttribute } from "./ModelAttribute"

// Add a new datasetmodel
export const ADD_DATASET_MODEL = 'ADD_DATASET_MODEL'

export const addDatasetModel = (id: number, name: string, attributes: Array<ModelAttribute>) => ({
  type: ADD_DATASET_MODEL,
  entity: {
    id,
    attributes,
    name
  }
})
