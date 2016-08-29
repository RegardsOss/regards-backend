import { ModelAttribute } from "./ModelAttribute"
export interface Model {
  name: string
  id?: number
  attributes: Array<ModelAttribute>
}
