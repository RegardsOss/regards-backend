import React, { PropTypes } from 'react'

const RegardsSelect = ({
  list = [],
  onSelect,
  identityAttribute = 'id',
  displayAttribute = 'name',
  label
}) => {
  return (
    <select onChange={onSelect}>
      <option defaultValue>{label}</option>
      {list.map(item =>
        <option key={item[identityAttribute]} value={item[identityAttribute]}>{item[displayAttribute]}</option>
      )}
    </select>
  )
}

RegardsSelect.propTypes = {
  list: PropTypes.arrayOf(PropTypes.object),
  onSelect: PropTypes.func,
  identityAttribute: PropTypes.string,
  displayAttribute: PropTypes.string,
  label: PropTypes.string
}

export default RegardsSelect
