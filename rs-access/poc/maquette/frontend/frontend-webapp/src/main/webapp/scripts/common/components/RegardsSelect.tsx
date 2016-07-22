import * as React from 'react'
import { PropTypes } from 'react'
import SelectField from 'material-ui/SelectField';
import MenuItem from 'material-ui/MenuItem';

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
    <SelectField value={0} onChange={onSelect} autoWidth={true} >
      <MenuItem value={0} primaryText={label} />
      <MenuItem value={1} primaryText="Auto width" />
      <MenuItem value={2} primaryText="Every Night" />
      <MenuItem value={3} primaryText="Weeknights" />
      <MenuItem value={4} primaryText="Weekends" />
      <MenuItem value={5} primaryText="Weekly" />
    </SelectField>
  )
}

// <select onChange={onSelect}>
// <option defaultValue>{label}</option>
// {list.map(item =>
//   <option key={item[identityAttribute]} value={item[identityAttribute]}>{item[displayAttribute]}</option>
// )}
// </select>
export default RegardsSelect
