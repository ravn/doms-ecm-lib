<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:foxml="info:fedora/fedora-system:def/foxml#"
        >

    <xsl:output omit-xml-declaration="yes" indent="yes"/>
    <xsl:strip-space elements="*"/>


    <xsl:template match="//foxml:datastream">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="foxml:datastreamVersion[last()]"/>
        </xsl:copy>

    </xsl:template>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>


</xsl:stylesheet>