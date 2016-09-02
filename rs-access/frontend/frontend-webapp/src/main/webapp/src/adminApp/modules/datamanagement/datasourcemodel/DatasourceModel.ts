import { ModelAttribute } from "../datasetmodel/ModelAttribute"
export interface DatasourceModel {
  name: string
  id?: number
  attributes: Array<ModelAttribute>
}
