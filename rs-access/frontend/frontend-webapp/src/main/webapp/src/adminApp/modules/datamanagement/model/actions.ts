import { ModelAttribute } from "./ModelAttribute"

// Add a new model
export const ADD_MODEL = 'ADD_MODEL'

export const addModel = (id: number, name: string, attributes: Array<ModelAttribute>) => ({
  type: ADD_MODEL,
  entity: {
    id,
    attributes,
    name
  }
})
