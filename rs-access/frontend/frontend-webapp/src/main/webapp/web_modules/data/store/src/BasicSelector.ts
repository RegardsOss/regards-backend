export class BasicSelectors {
  rootStore: any

  constructor (rootStore: Array<string>) {
    this.rootStore = rootStore
  }

  /**
   * Returns the subset of the store that your reducer is based on
   * @param store
   * @returns {any}
   */
  uncombineStore (store: any): any {
    let partialStore: any
    try {
      for (let i = 0; i < this.rootStore.length; i++) {
        partialStore = partialStore[this.rootStore[i]]
      }
    } catch (e) {
      throw new Error('Failed to uncombine the store with following ${this.rootStore} while selecting data on the store')
    }
    return partialStore
  }
}
