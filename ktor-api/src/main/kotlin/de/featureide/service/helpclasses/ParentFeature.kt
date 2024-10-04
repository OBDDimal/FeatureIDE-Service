package de.featureide.service.helpclasses

import de.ovgu.featureide.fm.core.base.IFeature
import de.ovgu.featureide.fm.core.base.IFeatureStructure
import de.ovgu.featureide.fm.core.base.impl.Feature
import de.ovgu.featureide.fm.core.base.impl.FeatureStructure

class ParentFeature (val feature: IFeature, val commonality: Double, val featureStructure: IFeatureStructure, var childrenConstraints: Int = 0,
                     var childrenChildren: Int = 0 ){
}