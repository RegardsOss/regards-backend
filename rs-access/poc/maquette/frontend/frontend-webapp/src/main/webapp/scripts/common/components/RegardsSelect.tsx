import * as React from 'react'
import { PropTypes } from 'react'

interface RegardsSelectProps {
  list: Array<any>,
  onSelect: ()=> void,
  identityAttribute: string,
  displayAttribute: string,
  label: string
}

const RegardsSelect = ({
  list = [],
  onSelect,
  identityAttribute = 'id',
  displayAttribute = 'name',
  label
}: RegardsSelectProps) => {
  return (
    <select onChange={onSelect}>
      <option defaultValue>{label}</option>
      {list.map(item =>
        <option key={item[identityAttribute]} value={item[identityAttribute]}>{item[displayAttribute]}</option>
      )}
    </select>
  )
}

export default RegardsSelect
