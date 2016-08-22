
export const SET_DATASOURCE = 'dataset/SET_DATASOURCE'
export function setDatasource(idDatasource: number): Object {
  return {
    type: SET_DATASOURCE,
    idDatasource: idDatasource
  }
}


export const SET_VIEW_STATE = 'dataset/SET_VIEW_STATE'
export function setViewState(viewState: string): Object {
  return {
    type: SET_VIEW_STATE,
    viewState: viewState
  }
}

// Todo: use Dataset type instead of any
export const SET_DATASET_ATTRIBUTES = 'dataset/SET_DATASET_ATTRIBUTES'
export function setDatasetAttributes(datasetAttributes: any): Object {
  return {
    type: SET_VIEW_STATE,
    datasetAttributes: datasetAttributes
  }
}
