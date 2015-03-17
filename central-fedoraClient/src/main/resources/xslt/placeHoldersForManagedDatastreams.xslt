<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:foxml="info:fedora/fedora-system:def/foxml#"
        >

    <xsl:output omit-xml-declaration="yes" indent="yes"/>
    <xsl:strip-space elements="*"/>

    <xsl:template match="foxml:datastream[@CONTROL_GROUP='M']/foxml:datastreamVersion[@MIMETYPE='text/xml']">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:element name="foxml:xmlContent" namespace="info:fedora/fedora-system:def/foxml#">
                PLACEHOLDER_PREFIX<xsl:value-of select="../@ID"/>PLACEHOLDER_POSTFIX
            </xsl:element>

        </xsl:copy>
    </xsl:template>


    <!--Identity copy-->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>