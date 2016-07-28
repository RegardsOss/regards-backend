# Software Reuse File
|               | Name                      | Company | Responsibility   |
| ------------- | :-----------------------: | :-----: | :--------------: |
| Written by    | Xavier-Alexandre Brochard | CS      | Development Team |
| Verified by   |                           |         |                  |
| Approved by   |                           |         |                  ||
## Document Status Sheet
| Issue | Date        | Reasons for change       |
| ------| :---------: | :----------------------: |
| 00    | 2016-07-28  | Creation of the document |
|       |             |                          ||
## Table of contents
1. [Introduction](#introduction)
  1. [Purpose of the document](#purpose-of-the-document)
  2. [Scope](#scope)
  3. [Document structure](#document-structure)
  4. [References](#references)
  1. [Applicable and Reference documents](#applicable-and-reference-documents)
    2. [Definitions](#definitions)
    2. [Third party products and required software licences](#third-party-products-and-required-software-licences)
  1. [General](#general)
    1. [Free licence categorization and meaning](#free-licence-categorization-and-meaning)
    2. [Impact of free licences on customers](#impact-of-free-licences-on-customers)
  2. [React](#react)
  3. [Redux](#redux)
3. [Rights to use, copy, modify, distribute for each software component](#rights-to-use,-copy,-modify,-distribute-for-each-software-component)

### 1. Introduction

#### 1. Purpose of the document

This document, Software Reuse File (SRF), describes any item of software, which it
proposes for reuse.

It explains the reason why the software is proposed for reuse, where and the extent to which the software would be integrated in the software deliverables, the ownership of the software item and the license conditions on which the software could be used by the CNES or a third party during the contract, and after the contract’s conclusion.

#### 2. Scope

The document describes the software to be re-used during the pre-development phase (proof-of-concepts phase) of REGARDS.

#### 3. Document structure

The document is structured as follows:
* This chapter gives the purpose and the structure of the document and the list of
references: applicable and reference documents and definitions.
* Chapter 2 provides in a first part a detailed depiction of the free licenses categorization and impacts on users, and in a second part an analysis of the 3rd party products reused in REGARDS.

#### 4. References

##### 1. Applicable and Reference documents

Applicable and reference documents are:

| Reference             | Acronym | Title | Version | Date |
| --------------------- | :-----: | :---: | :-----: | :--: |
| SGDS-SL-12100-0002-CS | TODO    | DOSSIER DE SPECIFICATION LOGICIEL REGARDS     | 01 | 25/03/2016 |
| SGDS-CP-12200-0010-CS | TODO    | Dossier de Conception Préliminaire du REGARDS | 00 | 01/06/2016 ||

##### 2. Definitions

TODO

### 2. Third party products and required software licences

This chapter provides an analysis of the 3rd party products reused in the REGARDS.

This chapter also identifies required Software Licenses, and lists all the development and documentation production tools.

However, before describing these elements, we introduce a reminder on free license products, in order to categorise the impact on any actor and other third party use during and after the project.

#### 1. General

##### 1. Free licence categorization and meaning

Free license are generally classified into the following three main categories, according to ascending permissivity:
* Strong copyleft licenses (GPL, CeCILL)
* Weak copyleft licenses (LGPL)
* Permissive licences (BSD, MIT, Apache

All these licenses categories share some general features. They all **allow free use regardless of domain or country. They all allow redistribution. They all allow modification. They all
allow distribution of the modifications** (these are known as the four freedoms of free software). The categories differ in how redistributed code can be licensed if someone decides to exercise his right to redistribute.

**Strong copyleft** licenses like GPL or CeCILL mandates that derived products are redistributed under the same terms as the original FOSS component that is used to build the product. This means that an image processing filter built using a CeCILL licensed library will also be subject to the same CeCILL license. This characteristic of the strong copyleft licenses is sometimes known as a "reciprocal" property: if one uses code from someone under a copyleft license for building a product, one will also distribute this product under the same license so other people can also build something else on top of it.

**Weak copyleft** licenses like LGPL, EPL or CeCILL-C are similar in spirit but the license spreading feature can be limited to modification of the original code. As an example, if an image processing filter uses an LGPL based library and is linked to it using dynamic linking only, then only the changes to the library must be distributed under the terms of the LGPL and the complete program can be distributed under other license terms if desired. So the "weak" term refers to the fact license reciprocity is more limited.

**Permissive licenses** like MIT, BSD or Apache licenses do not mandate any licensing terms for derived product. This means an image processing filter built using an Apache license library can be distributed under any licenses terms, even if the original Apache code itself has been modified.

As seen, copyleft notion has to deal with distribution agreement. It is better identified as a "reciprocal" effect but may sometimes be negatively referred to as "viral propagation", "infection" or "contamination" in some cases. For instance, let’s consider a project which includes any amount of source code from free licensed product "A" and there is a need to make changes to some part of source code, corresponding to additional code "A’ ", on the one hand, plus a need to add a wrapping layer "B", on the other hand. "A" + "A’ ""+ "B" aiming to create a new "Alpha" application. "Alpha" product diffusion license can be chosen only according to "A" original license itself as explained hereafter:
* whenever "A" is distributed under the terms of a **strong** copyleft license, the entire new or modified pieces of code ("A’ " in this example) or derived work ("B") becomes subject to the terms of the original license,
* Whenever "A" is distributed under the terms of a **weak** copyleft license, in some cases only modified work becomes subject to the terms of the original license. Thus, whereas "A" and "A’ " will be subject to the terms of "A" original license, yet "B" may be submitted to another kind of license. Some conditions must be fulfilled in such a case: if both "A" and "A’ " are part of a **dynamically linked** library and the final user is given
the capability of replacing "A+A’ " in order to introduce his own modification ""A+A’+A’’ ", then "B" may be distributed under a different license. On the other side, if both "A" and "A’ " are part of a **statically linked** library, then "B" should be distributed under the terms of the same license. Exact conditions of distribution are described within the license terms themselves. A careful attention must be paid onto the distribution license version, either LGPL v2.1 or LGPL v3, which differ on this point
* whenever "A" is distributed under the terms of a **permissive** license, then "A’ " and "B" may be distributed under any kind of license, in fact, even "A" can be relicensed if needed

Distribution licenses type depends on two major characteristics.


First, the kind of distribution of a given license is conditioned by the intention or not to have it distributed to third parties or not. Thus, whenever developers use a given product, even modified, for private usage (private may be understood even within a firm), the derived product may be kept private and secret and need no specific license itself.  
However, whenever it is intended to distribute the product to third parties, then either updated or derived products should be distributed under free licenses as well.


Moreover, whenever one intends to distribute pieces of code under the terms of a copyleft license, then this distribution may be strictly limited to the product recipient alone. It is not mandatory to publish it on internet or to deliver it back to the original product former authors or community. Yet exception may be found in some cases (see later).

##### 2. Impact of free licences on customers

Customers may be led to change their distribution policy for some products originally developed for their own internal use with no initial intention to have them shared or edited. Whenever they decide later on to have these products finally distributed to other space agencies for instance or industrial, they have to reconsider the licensing terms of the included free software components.

In order to make this kind of distribution policy changes possible, one way is to avoid using **strong** copyleft components even for internal products. This prevents expensive developments to get rid of some restrictive COTS for instance and replace them by more permissive equivalent.

Therefore, CS proposes, when possible, to avoid **strong** copyleft COTS usage within its developments, such as GPL, AGPL, CeCILL and prefer **weak** copyleft (res. **permissive** license) such as LGPL, CeCILL-C (res. CeCILL-B, BSD, MIT, Apache).

Using **weak** copyleft licenses products without any change enables to guarantee that no code developed within the project should fall under distribution rules that may get incompatible to related intellectual property laws.

#### 2. React

| Feature | Value |
| ----- | :-----: |
| Name | React |
| Developer/Ownership | Facebook, Inc. |
| Licencing conditions | BSD License |
| Industrial Property Constraints | Redistribution and use in source and binary forms, with or without modification, are permitted provided [those conditions](#https://github.com/facebook/react/blob/master/LICENSE). |
| Applicable dispositions for maintenance, installation and training | todo |
| Commercial SW needed for execution | todo |
| Development and execution environment | todo |
| Version and components | 15.0.2 |
| Language | JavaScript, C++, TypeScript, CoffeScript, Python, C |
| Size | todo |

#### 3. Redux

| Feature | Value |
| ----- | :-----: |
| Name | Redux |
| Developer/Ownership | Dan Abramov |
| Licencing conditions | MIT License (MIT) |
| Industrial Property Constraints | Permission is granted, free of charge, to deal in the Software without restriction, subject to [those conditions](#https://github.com/reactjs/redux/blob/master/LICENSE.md) |
| Applicable dispositions for maintenance, installation and training | todo |
| Commercial SW needed for execution | todo |
| Development and execution environment | todo |
| Version and components | 3.5.2 |
| Language | JavaScript, TypeScript |
| Size | todo |

#### 4. Material-UI

| Feature | Value |
| ----- | :-----: |
| Name | Material-UI |
| Developer/Ownership | Call-Em-All |
| Licencing conditions | MIT License (MIT) |
| Industrial Property Constraints | Permission is granted, free of charge, to deal in the Software without restriction, subject to [those conditions](#https://github.com/callemall/material-ui/blob/master/LICENSE) |
| Applicable dispositions for maintenance, installation and training | todo |
| Commercial SW needed for execution | todo |
| Development and execution environment | todo |
| Version and components | 0.15.2 |
| Language | JavaScript |
| Size | todo |

### 3. Rights to use, copy, modify, distribute for each software component

| Software component | Use | Copy | Modify | Distribute |
| --- | :---: | :---: | :---: | :---: |
| React | Yes | Yes | Yes | Yes |
| Redux | Yes | Yes | Yes | Yes |
| Material-UI | Yes | Yes | Yes | Yes |
