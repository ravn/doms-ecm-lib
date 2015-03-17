<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:foxml="info:fedora/fedora-system:def/foxml#"
        >

    <xsl:output omit-xml-declaration="yes" indent="yes" method="text"/>
    <xsl:strip-space elements="*" />

    <xsl:template match="foxml:datastream[@CONTROL_GROUP='M']/foxml:datastreamVersion[@MIMETYPE='text/xml']">
        <xsl:value-of select="../@ID"/>
        <xsl:text>&#xA;</xsl:text>
    </xsl:template>


    <xsl:template match="@*|node()">
        <xsl:apply-templates select="@*|node()"/>
    </xsl:template>


</xsl:stylesheet>