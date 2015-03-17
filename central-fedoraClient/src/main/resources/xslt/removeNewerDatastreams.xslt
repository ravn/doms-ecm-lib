<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:foxml="info:fedora/fedora-system:def/foxml#"
        >

    <xsl:output omit-xml-declaration="yes" indent="yes"/>
    <xsl:strip-space elements="*"/>
    <xsl:param name="highestCreated"/>

    <xsl:template match="//foxml:datastream/foxml:datastreamVersion[translate(@CREATED,'-T:.Z','') > $highestCreated]">

    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>


</xsl:stylesheet>