<!--
SPDX-FileCopyrightText: 'Copyright Contributors to the schema-composer project' 

SPDX-License-Identifier: Apache-2.0
-->

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
# SchemaComposer
The goal of the tool is to generate/build profiles and related artifact based on UML models and OWL ontologies. SchemaComposer allows users to create logical models visually, using concepts from conceptual models defined in both the Web Ontology Language (OWL) and the Unified Modeling Language (UML). Using these logical models, schemas in various schema definition languages can be generated automatically. Currently supports the generation of Avro Schema and JSON Schema. Idea's for other formats include: Odata, OAS, CIM-RDF, XSD, GraphQL etc.

## General working of the tool
The tools allows to select OWL or UML class concepts, set some additonal contrains (e.g. select a root class) and export them to an Avro or JSON schema.

### Conceptual model import options
The ESM schema composer supports 2 ways of imports

#### Enterprise architect pro-cloud server API
The ESM schema composer uses the Enterprise architect database to retrieve UML classes. The tool does not store a copy of the UML profile. It only retrieves the relevant parts.

#### Manual OWL import 
The tool allows to import OWL files.

## Architecture overview
[![Architecture overview](./Architecture-of-hte-schema-composer.png "Architecture")]

# Status: Prototype

# License
This project is licensed under the Apache 2.0 - see [LICENSE](LICENSE) for details.

# Licenses third-party libraries
This project includes third-party libraries, 
which are licensed under their own respective Open-Source licenses.
SPDX-License-Identifier headers are used to show which license is applicable. 
The concerning license files can be found in the [LICENSES](LICENSES) directory.

# Contributing
Please read [CODE_OF_CONDUCT](CODE_OF_CONDUCT.md) and [CONTRIBUTING](CONTRIBUTING.md) for details on the process 
for submitting pull requests to us.

# Contact
Please read [SUPPORT](SUPPORT.md) for how to connect and get into contact with the Power Gird Model project.
